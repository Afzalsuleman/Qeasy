package com.smartqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Smart Queue Management System - Main Application
 *
 * A backend service that enables shops to manage customer queues digitally,
 * reducing physical wait times and improving customer experience.
 *
 * Key Features:
 * - OTP-based Authentication (Email via Gmail SMTP)
 * - Shop Management
 * - Real-time Queue Management (Redis + WebSocket)
 * - Auto no-show handling (3-minute timeout)
 * - Wait time estimation
 * - Audit trail (30-day retention)
 *
 * @version 1.0.0
 * @since 2026-01-04
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaAuditing
public class SmartQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartQueueApplication.class, args);
    }
}
