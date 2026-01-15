package com.smartqueue.repository;

import com.smartqueue.model.entity.QueueLog;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.QueueStatus;
import com.smartqueue.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for QueueLogRepository
 * Tests database queries and data persistence
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("QueueLogRepository Tests")
class QueueLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueueLogRepository queueLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User testUser;
    private User testUser2;
    private Shop testShop;
    private QueueLog queueLog1;
    private QueueLog queueLog2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("user1@example.com")
                .name("Test User 1")
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        testUser2 = User.builder()
                .email("user2@example.com")
                .name("Test User 2")
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser2);

        // Create shop owner
        User shopOwner = User.builder()
                .email("owner@example.com")
                .name("Shop Owner")
                .role(UserRole.SHOP_OWNER)
                .build();
        userRepository.save(shopOwner);

        // Create test shop
        testShop = Shop.builder()
                .owner(shopOwner)
                .name("Test Shop")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(10)
                .isActive(true)
                .build();
        shopRepository.save(testShop);

        // Create queue logs
        Instant now = Instant.now();

        queueLog1 = QueueLog.builder()
                .shop(testShop)
                .user(testUser)
                .status(QueueStatus.JOINED)
                .joinedAt(now)
                .build();
        queueLogRepository.save(queueLog1);

        queueLog2 = QueueLog.builder()
                .shop(testShop)
                .user(testUser2)
                .status(QueueStatus.CALLED)
                .joinedAt(now.minus(10, ChronoUnit.MINUTES))
                .calledAt(now)
                .build();
        queueLogRepository.save(queueLog2);
    }

    @Test
    @DisplayName("Should find queue logs by shop and status")
    void shouldFindQueueLogsByShopAndStatus() {
        // When
        List<QueueLog> joinedLogs = queueLogRepository.findByShopIdAndStatus(testShop.getId(), QueueStatus.JOINED);
        List<QueueLog> calledLogs = queueLogRepository.findByShopIdAndStatus(testShop.getId(), QueueStatus.CALLED);

        // Then
        assertThat(joinedLogs).hasSize(1);
        assertThat(joinedLogs.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(calledLogs).hasSize(1);
        assertThat(calledLogs.get(0).getUser().getId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("Should count queue logs by shop and date range")
    void shouldCountQueueLogsByShopAndDateRange() {
        // Given
        Instant now = Instant.now();
        Instant startDate = now.minus(1, ChronoUnit.HOURS);
        Instant endDate = now.plus(1, ChronoUnit.HOURS);

        // When
        long count = queueLogRepository.countByShopIdAndDateRange(testShop.getId(), startDate, endDate);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count queue logs by shop, status, and date range")
    void shouldCountByShopStatusAndDateRange() {
        // Given
        Instant now = Instant.now();
        Instant startDate = now.minus(1, ChronoUnit.HOURS);
        Instant endDate = now.plus(1, ChronoUnit.HOURS);

        // When
        long joinedCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                testShop.getId(), QueueStatus.JOINED, startDate, endDate);
        long calledCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                testShop.getId(), QueueStatus.CALLED, startDate, endDate);

        // Then
        assertThat(joinedCount).isEqualTo(1);
        assertThat(calledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find latest queue log for user and shop")
    void shouldFindLatestQueueLogForUserAndShop() {
        // When
        Optional<QueueLog> latestLog = queueLogRepository.findLatestByUserIdAndShopId(testUser.getId(), testShop.getId());

        // Then
        assertThat(latestLog).isPresent();
        assertThat(latestLog.get().getStatus()).isEqualTo(QueueStatus.JOINED);
    }

    @Test
    @DisplayName("Should find queue log by user, shop, and status")
    void shouldFindQueueLogByUserShopAndStatus() {
        // When
        Optional<QueueLog> log = queueLogRepository.findByShopIdAndUserIdAndStatus(
                testShop.getId(), testUser.getId(), QueueStatus.JOINED);

        // Then
        assertThat(log).isPresent();
        assertThat(log.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should get average wait time")
    void shouldGetAverageWaitTime() {
        // Given
        // queueLog2 was called after 10 minutes
        Instant now = Instant.now();
        Instant startDate = now.minus(1, ChronoUnit.HOURS);
        Instant endDate = now.plus(1, ChronoUnit.HOURS);

        // When
        Double avgWaitTime = queueLogRepository.getAverageWaitTime(testShop.getId(), startDate, endDate);

        // Then
        assertThat(avgWaitTime).isNotNull();
        // Average is based on the difference between joinedAt and calledAt
        assertThat(avgWaitTime).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find queue logs by status and called before time")
    void shouldFindQueueLogsByStatusAndCalledBefore() {
        // Given
        // queueLog2 was called in the past
        Instant now = Instant.now();
        Instant calledBefore = now.plus(1, ChronoUnit.HOURS); // Everything should be before this

        // When
        List<QueueLog> logs = queueLogRepository.findByStatusAndCalledAtBefore(QueueStatus.CALLED, calledBefore);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(QueueStatus.CALLED);
    }

    @Test
    @DisplayName("Should delete queue logs older than cutoff date")
    void shouldDeleteQueueLogsOlderThanCutoff() {
        // Given
        Instant now = Instant.now();
        Instant cutoffDate = now.plus(1, ChronoUnit.HOURS); // All logs are older than this

        long initialCount = queueLogRepository.count();
        assertThat(initialCount).isEqualTo(2);

        // When
        int deletedCount = queueLogRepository.deleteByJoinedAtBefore(cutoffDate);
        entityManager.flush();

        // Then
        assertThat(deletedCount).isEqualTo(2);
        assertThat(queueLogRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle empty date range correctly")
    void shouldHandleEmptyDateRange() {
        // Given
        Instant now = Instant.now();
        Instant startDate = now.plus(10, ChronoUnit.DAYS);
        Instant endDate = now.plus(20, ChronoUnit.DAYS);

        // When
        long count = queueLogRepository.countByShopIdAndDateRange(testShop.getId(), startDate, endDate);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle multiple users correctly")
    void shouldHandleMultipleUsersCorrectly() {
        // Given - already have 2 users in setup
        // When
        List<QueueLog> userLogs = queueLogRepository.findByShopIdAndStatus(testShop.getId(), QueueStatus.JOINED);
        List<QueueLog> user2Logs = queueLogRepository.findByShopIdAndStatus(testShop.getId(), QueueStatus.CALLED);

        // Then
        assertThat(userLogs).hasSize(1);
        assertThat(user2Logs).hasSize(1);
        assertThat(userLogs.get(0).getUser().getId()).isNotEqualTo(user2Logs.get(0).getUser().getId());
    }

    @Test
    @DisplayName("Should persist all queue log fields")
    void shouldPersistAllQueueLogFields() {
        // When
        Optional<QueueLog> retrieved = queueLogRepository.findById(queueLog1.getId());

        // Then
        assertThat(retrieved).isPresent();
        QueueLog log = retrieved.get();
        assertThat(log.getShop().getId()).isEqualTo(testShop.getId());
        assertThat(log.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(log.getStatus()).isEqualTo(QueueStatus.JOINED);
        assertThat(log.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should support queue log updates")
    void shouldSupportQueueLogUpdates() {
        // Given
        queueLog1.setStatus(QueueStatus.CALLED);
        queueLog1.setCalledAt(Instant.now());

        // When
        queueLogRepository.save(queueLog1);
        QueueLog updated = queueLogRepository.findById(queueLog1.getId()).orElseThrow();

        // Then
        assertThat(updated.getStatus()).isEqualTo(QueueStatus.CALLED);
        assertThat(updated.getCalledAt()).isNotNull();
    }
}
