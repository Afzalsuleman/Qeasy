package com.smartqueue.security;

import com.smartqueue.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT Authentication Filter
 * Extracts and validates JWT token from Authorization header
 * Sets Spring Security authentication context if valid
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && StringUtils.hasText(jwt)) {
                // Validate token
                if (jwtUtil.validateToken(jwt)) {
                    // Extract user details from token
                    UUID userId = jwtUtil.getUserIdFromToken(jwt);
                    String email = jwtUtil.getEmailFromToken(jwt);
                    String role = jwtUtil.getRoleFromToken(jwt);

                    // Build authorities list from role
                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                    if (role != null && !role.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }

                    // Create authentication token with authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    authorities
                            );

                    // Store userId and other details
                    authentication.setDetails(new UserAuthenticationDetails(
                            userId,
                            email,
                            jwtUtil.getNameFromToken(jwt),
                            role,
                            request
                    ));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {} (ID: {}, Role: {})", email, userId, role);
                }
            }
        } catch (InvalidTokenException ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage());
            // Don't throw exception - let request continue and fail at @PreAuthorize
        } catch (Exception ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     *
     * @param request HTTP request
     * @return JWT token string or null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Custom authentication details to store user information
     */
    public static class UserAuthenticationDetails extends WebAuthenticationDetailsSource {
        private final UUID userId;
        private final String email;
        private final String name;
        private final String role;

        public UserAuthenticationDetails(UUID userId, String email, String name, String role, HttpServletRequest request) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.role = role;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }
    }
}
