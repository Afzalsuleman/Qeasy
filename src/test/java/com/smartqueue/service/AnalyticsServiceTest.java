package com.smartqueue.service;

import com.smartqueue.exception.ShopNotFoundException;
import com.smartqueue.model.dto.AnalyticsResponse;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.enums.QueueStatus;
import com.smartqueue.repository.QueueLogRepository;
import com.smartqueue.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService
 * Tests queue statistics and analytics calculations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private QueueLogRepository queueLogRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Shop testShop;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        testShop = Shop.builder()
                .id(shopId)
                .name("Test Shop")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(10)
                .isActive(true)
                .build();

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("Should get current queue stats successfully")
    void shouldGetCurrentQueueStats() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard("queue:" + shopId + ":waiting")).thenReturn(4L);
        when(zSetOperations.zCard("queue:" + shopId + ":is_completed")).thenReturn(1L);

        // When
        AnalyticsResponse response = analyticsService.getCurrentQueueStats(shopId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCurrentQueueSize()).isEqualTo(5); // 4 waiting + 1 called
        assertThat(response.getJoinedCount()).isEqualTo(4);
        assertThat(response.getCalledCount()).isEqualTo(1);
        assertThat(response.getEstimatedWaitTimeMinutes()).isEqualTo(40); // (5-1) * 10
    }

    @Test
    @DisplayName("Should handle empty queue")
    void shouldHandleEmptyQueue() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getCurrentQueueStats(shopId);

        // Then
        assertThat(response.getCurrentQueueSize()).isEqualTo(0);
        assertThat(response.getJoinedCount()).isEqualTo(0);
        assertThat(response.getCalledCount()).isEqualTo(0);
        assertThat(response.getEstimatedWaitTimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw exception when shop not found")
    void shouldThrowExceptionWhenShopNotFound() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> analyticsService.getCurrentQueueStats(shopId))
                .isInstanceOf(ShopNotFoundException.class);
    }

    @Test
    @DisplayName("Should get shop analytics for date range")
    void shouldGetShopAnalyticsForDateRange() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(100L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.SERVED), any(), any()))
                .thenReturn(85L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.NO_SHOW), any(), any()))
                .thenReturn(10L);
        when(queueLogRepository.getAverageWaitTime(any(), any(), any())).thenReturn(12.5);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getShopAnalytics(shopId, 7);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalVisitors()).isEqualTo(100);
        assertThat(response.getServedCount()).isEqualTo(85);
        assertThat(response.getNoShowCount()).isEqualTo(10);
        assertThat(response.getAverageWaitTimeMinutes()).isEqualTo(12);
        assertThat(response.getCompletionRate()).isEqualTo(85.0); // 85/100 * 100
        assertThat(response.getNoShowRate()).isCloseTo(10.0, within(0.1)); // 10/100 * 100
        assertThat(response.getAnalyzedDays()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should calculate completion rate correctly")
    void shouldCalculateCompletionRateCorrectly() {
        // Given: 150 total, 120 served
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(150L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.SERVED), any(), any()))
                .thenReturn(120L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.NO_SHOW), any(), any()))
                .thenReturn(30L);
        when(queueLogRepository.getAverageWaitTime(any(), any(), any())).thenReturn(15.0);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getShopAnalytics(shopId, 7);

        // Then
        assertThat(response.getCompletionRate()).isEqualTo(80.0); // 120/150 * 100
        assertThat(response.getNoShowRate()).isEqualTo(20.0); // 30/150 * 100
    }

    @Test
    @DisplayName("Should handle zero visitors case")
    void shouldHandleZeroVisitors() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(0L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), any(), any(), any())).thenReturn(0L);
        when(queueLogRepository.getAverageWaitTime(any(), any(), any())).thenReturn(null);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getShopAnalytics(shopId, 7);

        // Then
        assertThat(response.getTotalVisitors()).isEqualTo(0);
        assertThat(response.getCompletionRate()).isEqualTo(0.0);
        assertThat(response.getNoShowRate()).isEqualTo(0.0);
        assertThat(response.getAverageWaitTimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should get today's statistics")
    void shouldGetTodayStats() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(20L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.SERVED), any(), any()))
                .thenReturn(18L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), eq(QueueStatus.NO_SHOW), any(), any()))
                .thenReturn(2L);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getTodayStats(shopId);

        // Then
        assertThat(response.getTotalVisitors()).isEqualTo(20);
        assertThat(response.getServedCount()).isEqualTo(18);
        assertThat(response.getNoShowCount()).isEqualTo(2);
        assertThat(response.getAnalyzedDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should include date range in analytics response")
    void shouldIncludeDateRangeInResponse() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(50L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), any(), any(), any())).thenReturn(40L);
        when(queueLogRepository.getAverageWaitTime(any(), any(), any())).thenReturn(10.0);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getShopAnalytics(shopId, 7);

        // Then
        assertThat(response.getStartDate()).isNotNull();
        assertThat(response.getEndDate()).isNotNull();
        assertThat(response.getStartDate()).isBefore(response.getEndDate());
    }

    @Test
    @DisplayName("Should calculate broadcast stats correctly")
    void shouldCalculateBroadcastStatsCorrectly() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard("queue:" + shopId + ":waiting")).thenReturn(3L);
        when(zSetOperations.zCard("queue:" + shopId + ":is_completed")).thenReturn(1L);

        // When
        AnalyticsResponse response = analyticsService.getCurrentQueueStatsForBroadcast(shopId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCurrentQueueSize()).isEqualTo(4);
        assertThat(response.getJoinedCount()).isEqualTo(3);
        assertThat(response.getCalledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle null average wait time from database")
    void shouldHandleNullAverageWaitTime() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(queueLogRepository.countByShopIdAndDateRange(any(), any(), any())).thenReturn(50L);
        when(queueLogRepository.countByShopIdAndStatusAndDateRange(any(), any(), any(), any())).thenReturn(40L);
        when(queueLogRepository.getAverageWaitTime(any(), any(), any())).thenReturn(null); // No data
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getShopAnalytics(shopId, 7);

        // Then
        assertThat(response.getAverageWaitTimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should include shop info in response")
    void shouldIncludeShopInfoInResponse() {
        // Given
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // When
        AnalyticsResponse response = analyticsService.getCurrentQueueStats(shopId);

        // Then
        assertThat(response.getShopId()).isEqualTo(shopId);
        assertThat(response.getShopName()).isEqualTo("Test Shop");
        assertThat(response.getMaxQueueSize()).isEqualTo(50);
        assertThat(response.getAvgServiceTimeMinutes()).isEqualTo(10);
    }
}
