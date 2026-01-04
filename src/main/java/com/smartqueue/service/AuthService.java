package com.smartqueue.service;

import com.smartqueue.exception.InvalidOtpException;
import com.smartqueue.model.dto.AuthResponse;
import com.smartqueue.model.dto.GenerateOtpRequest;
import com.smartqueue.model.dto.VerifyOtpRequest;
import com.smartqueue.model.entity.User;
import com.smartqueue.repository.UserRepository;
import com.smartqueue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Authentication service handling OTP generation and verification
 * Uses Redis for OTP storage with 5-minute expiry
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    private static final String REDIS_OTP_PREFIX = "otp:";
    private static final String REDIS_OTP_ATTEMPTS_PREFIX = "otp:attempts:";
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate and send OTP to user's email
     *
     * @param request GenerateOtpRequest with email and name
     * @return AuthResponse with success message
     */
    @Transactional
    public AuthResponse generateOtp(GenerateOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String name = request.getName().trim();

        log.info("Generating OTP for email: {}", email);

        // Check if too many recent OTP requests
        String attemptsKey = REDIS_OTP_ATTEMPTS_PREFIX + email;
        String attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsValue != null ? Integer.parseInt(attemptsValue) : 0;

        if (attempts >= MAX_OTP_ATTEMPTS) {
            log.warn("Too many OTP attempts for email: {}", email);
            emailService.lockEmail(email);
            throw new InvalidOtpException("Too many OTP requests. Please try again after 5 minutes.");
        }

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new user for email: {}", email);
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .build();
                    return userRepository.save(newUser);
                });

        // Update user name if changed
        if (!user.getName().equals(name)) {
            user.setName(name);
            userRepository.save(user);
        }

        // Generate OTP
        String otp = generateRandomOtp();

        // Store OTP in Redis with expiry
        String otpKey = REDIS_OTP_PREFIX + email;
        redisTemplate.opsForValue().set(otpKey, otp, otpExpiryMinutes, TimeUnit.MINUTES);

        // Increment attempts counter
        redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, otpExpiryMinutes, TimeUnit.MINUTES);

        // Send OTP email asynchronously
        emailService.sendOtpEmail(email, otp, name);

        log.info("OTP generated and email sent for: {}", email);

        return AuthResponse.builder()
                .message(String.format("OTP sent successfully to %s. Valid for %d minutes.",
                        email, otpExpiryMinutes))
                .build();
    }

    /**
     * Verify OTP and return JWT token
     *
     * @param request VerifyOtpRequest with email and otp
     * @return AuthResponse with user details and JWT token
     * @throws InvalidOtpException if OTP is invalid or expired
     */
    @Transactional(readOnly = true)
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String providedOtp = request.getOtp().trim();

        log.info("Verifying OTP for email: {}", email);

        // Get stored OTP from Redis
        String otpKey = REDIS_OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            log.warn("OTP not found or expired for email: {}", email);
            throw new InvalidOtpException("OTP has expired or does not exist. Please request a new one.");
        }

        // Verify OTP
        if (!storedOtp.equals(providedOtp)) {
            log.warn("Invalid OTP provided for email: {}", email);
            throw new InvalidOtpException("Invalid OTP. Please check and try again.");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("User not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        // Delete OTP and attempts from Redis (one-time use)
        redisTemplate.delete(otpKey);
        redisTemplate.delete(REDIS_OTP_ATTEMPTS_PREFIX + email);

        log.info("OTP verified successfully for email: {}", email);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .token(token)
                .expiresIn(jwtExpirationMs)
                .message("Authentication successful")
                .build();
    }

    /**
     * Generate a random OTP of specified length
     *
     * @return OTP string
     */
    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Check if OTP exists for email (for testing)
     *
     * @param email User email
     * @return true if OTP exists
     */
    public boolean hasValidOtp(String email) {
        String otpKey = REDIS_OTP_PREFIX + email.toLowerCase().trim();
        return Boolean.TRUE.equals(redisTemplate.hasKey(otpKey));
    }
}
