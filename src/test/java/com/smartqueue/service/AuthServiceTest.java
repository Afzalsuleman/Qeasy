package com.smartqueue.service;

import com.smartqueue.exception.InvalidOtpException;
import com.smartqueue.exception.UnauthorizedException;
import com.smartqueue.model.dto.AuthResponse;
import com.smartqueue.model.dto.GenerateOtpRequest;
import com.smartqueue.model.dto.VerifyOtpRequest;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.UserRole;
import com.smartqueue.repository.UserRepository;
import com.smartqueue.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests OTP generation, verification, and authentication flows
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private GenerateOtpRequest generateOtpRequest;
    private VerifyOtpRequest verifyOtpRequest;

    @BeforeEach
    void setUp() {
        // Set up private fields
        ReflectionTestUtils.setField(authService, "otpLength", 6);
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);

        // Configure RedisTemplate to return ValueOperations (lenient to avoid unnecessary stubbing warnings)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Set up test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();

        // Set up test requests
        generateOtpRequest = GenerateOtpRequest.builder()
                .email("test@example.com")
                .name("Test User")
                .build();

        verifyOtpRequest = VerifyOtpRequest.builder()
                .email("test@example.com")
                .otp("123456")
                .build();
    }

    @Test
    @DisplayName("Should generate OTP successfully for existing user")
    void shouldGenerateOtpForExistingUser() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn("0");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        AuthResponse response = authService.generateOtp(generateOtpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("OTP sent successfully");
        verify(valueOperations).set(eq("otp:test@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).increment("otp:attempts:test@example.com");
        verify(emailService).sendOtpEmail(eq("test@example.com"), anyString(), eq("Test User"));
    }

    @Test
    @DisplayName("Should create new user and generate OTP")
    void shouldCreateNewUserAndGenerateOtp() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        AuthResponse response = authService.generateOtp(generateOtpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("OTP sent successfully");
        verify(userRepository).save(any(User.class));
        verify(valueOperations).set(eq("otp:test@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(emailService).sendOtpEmail(eq("test@example.com"), anyString(), eq("Test User"));
    }

    @Test
    @DisplayName("Should update user name if changed")
    void shouldUpdateUserNameIfChanged() {
        // Given
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Old Name")
                .role(UserRole.USER)
                .build();

        GenerateOtpRequest requestWithNewName = GenerateOtpRequest.builder()
                .email("test@example.com")
                .name("New Name")
                .build();

        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        authService.generateOtp(requestWithNewName);

        // Then
        verify(userRepository).save(argThat(user -> user.getName().equals("New Name")));
    }

    @Test
    @DisplayName("Should throw exception when too many OTP attempts")
    void shouldThrowExceptionWhenTooManyAttempts() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn("3");
        doNothing().when(emailService).lockEmail("test@example.com");

        // When/Then
        assertThatThrownBy(() -> authService.generateOtp(generateOtpRequest))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Too many OTP requests");

        verify(emailService).lockEmail("test@example.com");
    }

    @Test
    @DisplayName("Should verify OTP successfully and return JWT token")
    void shouldVerifyOtpSuccessfully() {
        // Given
        String otp = "123456";
        String token = "jwt.token.here";

        when(valueOperations.get("otp:test@example.com")).thenReturn(otp);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn(token);

        // When
        AuthResponse response = authService.verifyOtp(verifyOtpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(token);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getMessage()).contains("Authentication successful");

        // Verify OTP and attempts are deleted (one-time use)
        verify(redisTemplate).delete("otp:test@example.com");
        verify(redisTemplate).delete("otp:attempts:test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when OTP not found or expired")
    void shouldThrowExceptionWhenOtpExpired() {
        // Given
        when(valueOperations.get("otp:test@example.com")).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> authService.verifyOtp(verifyOtpRequest))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should throw exception when OTP is invalid")
    void shouldThrowExceptionWhenOtpInvalid() {
        // Given
        when(valueOperations.get("otp:test@example.com")).thenReturn("654321"); // Different OTP

        // When/Then
        assertThatThrownBy(() -> authService.verifyOtp(verifyOtpRequest))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    @DisplayName("Should throw exception when user not found during OTP verification")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(valueOperations.get("otp:test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.verifyOtp(verifyOtpRequest))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should trim and lowercase email addresses")
    void shouldNormalizeEmailAddresses() {
        // Given
        GenerateOtpRequest requestWithMixedCase = GenerateOtpRequest.builder()
                .email("  Test@Example.COM  ")
                .name("Test User")
                .build();

        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        authService.generateOtp(requestWithMixedCase);

        // Then
        verify(valueOperations).set(eq("otp:test@example.com"), anyString(), anyLong(), any(TimeUnit.class));
        verify(emailService).sendOtpEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate 6-digit OTP")
    void shouldGenerate6DigitOtp() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        authService.generateOtp(generateOtpRequest);

        // Then
        verify(valueOperations).set(eq("otp:test@example.com"), argThat(otp ->
            otp != null && otp.length() == 6 && otp.matches("\\d{6}")
        ), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should check if valid OTP exists")
    void shouldCheckIfValidOtpExists() {
        // Given
        when(redisTemplate.hasKey("otp:test@example.com")).thenReturn(true);

        // When
        boolean hasOtp = authService.hasValidOtp("test@example.com");

        // Then
        assertThat(hasOtp).isTrue();
    }

    @Test
    @DisplayName("Should increment and expire attempts counter")
    void shouldIncrementAndExpireAttemptsCounter() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn("1");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        authService.generateOtp(generateOtpRequest);

        // Then
        verify(valueOperations).increment("otp:attempts:test@example.com");
        verify(redisTemplate).expire("otp:attempts:test@example.com", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("Should handle concurrent OTP requests")
    void shouldHandleConcurrentOtpRequests() {
        // Given
        when(valueOperations.get("otp:attempts:test@example.com")).thenReturn("2");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());

        // When
        authService.generateOtp(generateOtpRequest);

        // Then - Should not throw exception for 2 attempts (max is 3)
        verify(valueOperations).set(eq("otp:test@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(emailService, never()).lockEmail(anyString());
    }

    @Test
    @DisplayName("Should handle verification with extra whitespace in OTP")
    void shouldHandleWhitespaceInOtp() {
        // Given
        VerifyOtpRequest requestWithWhitespace = VerifyOtpRequest.builder()
                .email("test@example.com")
                .otp("  123456  ")
                .build();

        when(valueOperations.get("otp:test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("token");

        // When
        AuthResponse response = authService.verifyOtp(requestWithWhitespace);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
    }
}
