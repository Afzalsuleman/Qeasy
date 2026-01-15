package com.smartqueue.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.model.dto.GenerateOtpRequest;
import com.smartqueue.model.dto.VerifyOtpRequest;
import com.smartqueue.model.entity.User;
import com.smartqueue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication flow
 * Tests the complete OTP generation and verification flow with real dependencies
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up test data
        userRepository.deleteAll();
        // Clean up Redis keys
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("Should complete full authentication flow: generate OTP -> verify OTP -> get JWT token")
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        // Step 1: Generate OTP
        GenerateOtpRequest generateRequest = GenerateOtpRequest.builder()
                .email("integration@example.com")
                .name("Integration Test User")
                .build();

        mockMvc.perform(post("/api/v1/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("OTP sent successfully")));

        // Step 2: Verify user was created
        User createdUser = userRepository.findByEmail("integration@example.com").orElseThrow();
        assert createdUser != null;
        assert createdUser.getName().equals("Integration Test User");

        // Step 3: Get OTP from Redis
        String otp = redisTemplate.opsForValue().get("otp:integration@example.com");
        assert otp != null;
        assert otp.matches("\\d{6}"); // 6-digit OTP

        // Step 4: Verify OTP and get JWT token
        VerifyOtpRequest verifyRequest = VerifyOtpRequest.builder()
                .email("integration@example.com")
                .otp(otp)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.name").value("Integration Test User"))
                .andExpect(jsonPath("$.userId").value(notNullValue()))
                .andExpect(jsonPath("$.expiresIn").value(86400000L))
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        // Step 5: Verify OTP was deleted from Redis (one-time use)
        String deletedOtp = redisTemplate.opsForValue().get("otp:integration@example.com");
        assert deletedOtp == null;
    }

    @Test
    @DisplayName("Should fail verification with wrong OTP")
    void shouldFailVerificationWithWrongOtp() throws Exception {
        // Step 1: Generate OTP
        GenerateOtpRequest generateRequest = GenerateOtpRequest.builder()
                .email("wrong@example.com")
                .name("Wrong OTP Test")
                .build();

        mockMvc.perform(post("/api/v1/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateRequest)))
                .andExpect(status().isOk());

        // Step 2: Try to verify with wrong OTP
        VerifyOtpRequest verifyRequest = VerifyOtpRequest.builder()
                .email("wrong@example.com")
                .otp("999999") // Wrong OTP
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid OTP")));

        // Verify OTP still exists in Redis (not deleted on failed attempt)
        String otp = redisTemplate.opsForValue().get("otp:wrong@example.com");
        assert otp != null;
    }

    @Test
    @DisplayName("Should update existing user name on new OTP request")
    void shouldUpdateExistingUserName() throws Exception {
        // Step 1: Create user with initial name
        GenerateOtpRequest firstRequest = GenerateOtpRequest.builder()
                .email("update@example.com")
                .name("Initial Name")
                .build();

        mockMvc.perform(post("/api/v1/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        User user1 = userRepository.findByEmail("update@example.com").orElseThrow();
        assert user1.getName().equals("Initial Name");

        // Step 2: Request OTP again with updated name
        GenerateOtpRequest secondRequest = GenerateOtpRequest.builder()
                .email("update@example.com")
                .name("Updated Name")
                .build();

        mockMvc.perform(post("/api/v1/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk());

        // Step 3: Verify name was updated
        User user2 = userRepository.findByEmail("update@example.com").orElseThrow();
        assert user2.getName().equals("Updated Name");
        assert user2.getId().equals(user1.getId()); // Same user
    }
}
