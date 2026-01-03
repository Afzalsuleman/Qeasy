package com.smartqueue.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for request tracing
 * Generates or extracts X-Correlation-ID header for each request
 * Stores ID in MDC for logging, adds to response headers
 * As specified in tech-design-v3.md section 5.5
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = null;

        try {
            // Check if correlation ID exists in request header
            correlationId = request.getHeader(CORRELATION_ID_HEADER);

            // Generate new UUID if not present
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID from request: {}", correlationId);
            }

            // Store in MDC for logging (will be included in log pattern)
            MDC.put(MDC_KEY, correlationId);

            // Add to response header so client can reference it
            response.addHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Always clear MDC to prevent memory leaks
            MDC.remove(MDC_KEY);
            log.debug("Cleared correlation ID from MDC: {}", correlationId);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply filter to all requests
        return false;
    }
}
