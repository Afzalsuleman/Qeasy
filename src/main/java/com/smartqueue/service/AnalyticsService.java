package com.smartqueue.service;

import com.smartqueue.exception.ShopNotFoundException;
import com.smartqueue.model.dto.AnalyticsResponse;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.enums.QueueStatus;
import com.smartqueue.repository.QueueLogRepository;
import com.smartqueue.repository.ShopRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Analytics service for queue metrics and statistics
 * Provides insights into queue performance, wait times, and user behavior
 */
@Service
@Slf4j
public class AnalyticsService {

    private final ShopRepository shopRepository;
    private final QueueLogRepository queueLogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public AnalyticsService(
            ShopRepository shopRepository,
            QueueLogRepository queueLogRepository,
            RedisTemplate<String, String> redisTemplate) {
        this.shopRepository = shopRepository;
        this.queueLogRepository = queueLogRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get comprehensive analytics for a shop
     *
     * @param shopId Shop UUID
     * @param days Number of days to analyze (default: 7)
     * @return AnalyticsResponse with metrics
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getShopAnalytics(UUID shopId, int days) {
        log.info("Fetching analytics for shop {} (last {} days)", shopId, days);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Calculate date range
        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(days, ChronoUnit.DAYS);

        // Get current queue size from Redis
        String queueKey = "queue:" + shopId;
        Long currentQueueSize = redisTemplate.opsForZSet().zCard(queueKey);
        int currentQueue = currentQueueSize != null ? currentQueueSize.intValue() : 0;

        // Get total visitors in date range
        long totalVisitors = queueLogRepository.countByShopIdAndDateRange(shopId, startDate, endDate);

        // Get served count
        long servedCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                shopId, QueueStatus.SERVED, startDate, endDate);

        // Get no-show count
        long noShowCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                shopId, QueueStatus.NO_SHOW, startDate, endDate);

        // Get average wait time
        Double avgWaitTime = queueLogRepository.getAverageWaitTime(shopId, startDate, endDate);
        int averageWaitTimeMinutes = avgWaitTime != null ? avgWaitTime.intValue() : 0;

        // Calculate peak hours (simplified - count by hour would require more complex query)
        // For now, we'll provide basic stats

        // Calculate completion rate
        double completionRate = totalVisitors > 0
                ? (double) servedCount / totalVisitors * 100.0
                : 0.0;

        // Calculate no-show rate
        double noShowRate = totalVisitors > 0
                ? (double) noShowCount / totalVisitors * 100.0
                : 0.0;

        log.info("Analytics for shop {}: {} visitors, {} served, avg wait {} minutes",
                shopId, totalVisitors, servedCount, averageWaitTimeMinutes);

        return AnalyticsResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .currentQueueSize(currentQueue)
                .totalVisitors(totalVisitors)
                .servedCount(servedCount)
                .noShowCount(noShowCount)
                .averageWaitTimeMinutes(averageWaitTimeMinutes)
                .completionRate(completionRate)
                .noShowRate(noShowRate)
                .analyzedDays(days)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Get current queue statistics
     *
     * @param shopId Shop UUID
     * @return AnalyticsResponse with current queue metrics
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getCurrentQueueStats(UUID shopId) {
        log.info("Fetching current queue stats for shop {}", shopId);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get current queue size from Redis
        String queueKey = "queue:" + shopId;
        Long currentQueueSize = redisTemplate.opsForZSet().zCard(queueKey);
        int currentQueue = currentQueueSize != null ? currentQueueSize.intValue() : 0;

        // Calculate estimated wait time for last person
        int estimatedWaitTime = currentQueue > 0
                ? (currentQueue - 1) * shop.getAvgServiceTimeMinutes()
                : 0;

        return AnalyticsResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .currentQueueSize(currentQueue)
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .maxQueueSize(shop.getMaxQueueSize())
                .avgServiceTimeMinutes(shop.getAvgServiceTimeMinutes())
                .build();
    }

    /**
     * Get today's statistics for a shop
     *
     * @param shopId Shop UUID
     * @return AnalyticsResponse with today's metrics
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getTodayStats(UUID shopId) {
        log.info("Fetching today's stats for shop {}", shopId);

        // Calculate today's date range (midnight to now)
        Instant endDate = Instant.now();
        Instant startDate = endDate.truncatedTo(ChronoUnit.DAYS);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get counts
        long totalVisitors = queueLogRepository.countByShopIdAndDateRange(shopId, startDate, endDate);
        long servedCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                shopId, QueueStatus.SERVED, startDate, endDate);
        long noShowCount = queueLogRepository.countByShopIdAndStatusAndDateRange(
                shopId, QueueStatus.NO_SHOW, startDate, endDate);

        // Get current queue size
        String queueKey = "queue:" + shopId;
        Long currentQueueSize = redisTemplate.opsForZSet().zCard(queueKey);
        int currentQueue = currentQueueSize != null ? currentQueueSize.intValue() : 0;

        return AnalyticsResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .currentQueueSize(currentQueue)
                .totalVisitors(totalVisitors)
                .servedCount(servedCount)
                .noShowCount(noShowCount)
                .analyzedDays(1)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
