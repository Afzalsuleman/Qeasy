package com.smartqueue.security;

import com.smartqueue.exception.InvalidTokenException;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Tests token generation, validation, and claims extraction
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours
    private static final String TEST_ISSUER = "smart-queue-test";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "jwtIssuer", TEST_ISSUER);

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should validate generated token successfully")
    void shouldValidateGeneratedToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        UUID userId = jwtUtil.getUserIdFromToken(token);

        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String email = jwtUtil.getEmailFromToken(token);

        // Then
        assertThat(email).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract name from token")
    void shouldExtractNameFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String name = jwtUtil.getNameFromToken(token);

        // Then
        assertThat(name).isEqualTo(testUser.getName());
    }

    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String role = jwtUtil.getRoleFromToken(token);

        // Then
        assertThat(role).isEqualTo(testUser.getRole().name());
    }

    @Test
    @DisplayName("Should extract issuer from token")
    void shouldExtractIssuerFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String issuer = jwtUtil.getIssuerFromToken(token);

        // Then
        assertThat(issuer).isEqualTo(TEST_ISSUER);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpirationDateFromToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Date expiration = jwtUtil.getExpirationDateFromToken(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Given - Create token with -1 hour expiration
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -3600000L);
        String expiredToken = jwtUtil.generateToken(testUser);

        // Reset expiration for validation
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION);

        // When
        boolean isExpired = jwtUtil.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidTokenException for expired token validation")
    void shouldThrowExceptionForExpiredTokenValidation() {
        // Given - Create expired token
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -3600000L);
        String expiredToken = jwtUtil.generateToken(testUser);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION);

        // When/Then
        assertThatThrownBy(() -> jwtUtil.validateToken(expiredToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException for invalid signature")
    void shouldThrowExceptionForInvalidSignature() {
        // Given - Create token with different secret
        String differentSecret = "different-secret-key-must-be-at-least-256-bits-long-for-hs256";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithDifferentSignature = Jwts.builder()
                .setSubject(testUser.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION))
                .signWith(differentKey, SignatureAlgorithm.HS256)
                .compact();

        // When/Then
        assertThatThrownBy(() -> jwtUtil.validateToken(tokenWithDifferentSignature))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("signature");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When/Then
        assertThatThrownBy(() -> jwtUtil.validateToken(malformedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException for null token")
    void shouldThrowExceptionForNullToken() {
        // When/Then
        assertThatThrownBy(() -> jwtUtil.validateToken(null))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException for empty token")
    void shouldThrowExceptionForEmptyToken() {
        // When/Then
        assertThatThrownBy(() -> jwtUtil.validateToken(""))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("user1@example.com")
                .name("User One")
                .role(UserRole.USER)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("user2@example.com")
                .name("User Two")
                .role(UserRole.SHOP_OWNER)
                .build();

        // When
        String token1 = jwtUtil.generateToken(user1);
        String token2 = jwtUtil.generateToken(user2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.getEmailFromToken(token1)).isEqualTo(user1.getEmail());
        assertThat(jwtUtil.getEmailFromToken(token2)).isEqualTo(user2.getEmail());
    }

    @Test
    @DisplayName("Should handle SHOP_OWNER role correctly")
    void shouldHandleShopOwnerRole() {
        // Given
        User shopOwner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .name("Shop Owner")
                .role(UserRole.SHOP_OWNER)
                .build();

        // When
        String token = jwtUtil.generateToken(shopOwner);
        String role = jwtUtil.getRoleFromToken(token);

        // Then
        assertThat(role).isEqualTo("SHOP_OWNER");
    }

    @Test
    @DisplayName("Should generate token with correct expiration time")
    void shouldGenerateTokenWithCorrectExpiration() {
        // Given
        long beforeGeneration = System.currentTimeMillis();

        // When
        String token = jwtUtil.generateToken(testUser);
        Date expiration = jwtUtil.getExpirationDateFromToken(token);

        long afterGeneration = System.currentTimeMillis();
        long expectedExpiration = beforeGeneration + TEST_EXPIRATION;

        // Then
        assertThat(expiration.getTime()).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
    }
}
