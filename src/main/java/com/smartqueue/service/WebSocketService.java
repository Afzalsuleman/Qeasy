package com.smartqueue.service;

import com.smartqueue.model.dto.QueueResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * WebSocket service for broadcasting real-time queue updates
 * Sends messages to subscribed clients via STOMP
 */
@Service
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast queue update to all users subscribed to a shop's queue
     * Topic: /topic/queue/{shopId}
     *
     * @param shopId Shop UUID
     * @param queueUpdate QueueResponse with update details
     */
    public void broadcastQueueUpdate(UUID shopId, QueueResponse queueUpdate) {
        String destination = "/topic/queue/" + shopId;
        log.info("Broadcasting queue update to {}: {}", destination, queueUpdate.getStatus());
        messagingTemplate.convertAndSend(destination, queueUpdate);
    }

    /**
     * Send personal notification to a specific user
     * Queue: /queue/user/{userId}
     *
     * @param userId User UUID
     * @param notification QueueResponse with notification details
     */
    public void sendPersonalNotification(UUID userId, QueueResponse notification) {
        String destination = "/queue/user/" + userId;
        log.info("Sending personal notification to user {}: {}", userId, notification.getMessage());
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    /**
     * Notify user when they are called (next in line)
     *
     * @param userId User UUID
     * @param notification QueueResponse with call details
     */
    public void notifyUserCalled(UUID userId, QueueResponse notification) {
        log.info("Notifying user {} that they have been called", userId);
        sendPersonalNotification(userId, notification);
    }

    /**
     * Notify user of position update
     *
     * @param userId User UUID
     * @param queueUpdate QueueResponse with position update
     */
    public void notifyPositionUpdate(UUID userId, QueueResponse queueUpdate) {
        log.debug("Notifying user {} of position update: position {}", userId, queueUpdate.getPosition());
        sendPersonalNotification(userId, queueUpdate);
    }

    /**
     * Broadcast shop status change (active/inactive)
     *
     * @param shopId Shop UUID
     * @param isActive Shop status
     * @param message Status message
     */
    public void broadcastShopStatusChange(UUID shopId, boolean isActive, String message) {
        String destination = "/topic/shop/" + shopId + "/status";
        log.info("Broadcasting shop status change for {}: active={}", shopId, isActive);

        QueueResponse statusUpdate = QueueResponse.builder()
                .shopId(shopId)
                .status(isActive ? "ACTIVE" : "INACTIVE")
                .message(message)
                .build();

        messagingTemplate.convertAndSend(destination, statusUpdate);
    }

    /**
     * Broadcast queue statistics update
     *
     * @param shopId Shop UUID
     * @param totalInQueue Total users in queue
     * @param estimatedWaitTime Estimated wait time in minutes
     */
    public void broadcastQueueStats(UUID shopId, int totalInQueue, int estimatedWaitTime) {
        String destination = "/topic/queue/" + shopId + "/stats";

        QueueResponse stats = QueueResponse.builder()
                .shopId(shopId)
                .totalInQueue(totalInQueue)
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .status("STATS_UPDATE")
                .build();

        messagingTemplate.convertAndSend(destination, stats);
    }
}
