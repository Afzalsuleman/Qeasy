package com.smartqueue.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduled tasks and distributed locking
 * Uses ShedLock with Redis for distributed lock management
 * Ensures only one instance runs scheduled tasks in multi-instance deployments
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig {

    /**
     * Configure ShedLock with Redis provider
     * Locks are stored in Redis with a namespace prefix
     *
     * @param connectionFactory Redis connection factory
     * @return LockProvider for ShedLock
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "smart-queue");
    }
}
