package com.smartqueue.security;

import com.smartqueue.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * WebSocket authentication interceptor
 * Validates JWT token for WebSocket connections
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Autowired
    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Intercept messages before they are sent to the channel
     * Validate JWT token on CONNECT command
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract token from headers
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket connection attempt without valid Authorization header");
                throw new InvalidTokenException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("WebSocket connection attempt with invalid token");
                    throw new InvalidTokenException("Invalid JWT token");
                }

                // Extract user email from token
                String userEmail = jwtUtil.getEmailFromToken(token);

                // Set authentication in accessor for this WebSocket session
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userEmail, null, null);

                accessor.setUser(authentication);

                // Also set in SecurityContext for consistency
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("WebSocket connection authenticated for user: {}", userEmail);

            } catch (Exception e) {
                log.error("WebSocket authentication failed: {}", e.getMessage());
                throw new InvalidTokenException("Token validation failed: " + e.getMessage());
            }
        }

        return message;
    }
}
