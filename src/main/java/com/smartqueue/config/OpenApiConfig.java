package com.smartqueue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Swagger UI
 * Accessible at: http://localhost:8080/swagger-ui.html
 * API Docs JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI smartQueueOpenAPI() {
        // Define JWT security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from /api/v1/auth/verify-otp endpoint");

        // Define security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Smart Queue Management API")
                        .description("""
                                Backend API for digital queue management system in shops.

                                ## Authentication Flow
                                ### Option 1: OTP-Based Authentication (Regular Users)
                                1. Call POST /api/v1/auth/generate-otp with email and name
                                2. Check your email for the OTP code
                                3. Call POST /api/v1/auth/verify-otp with email and OTP
                                4. Copy the JWT token from the response
                                5. Click 'Authorize' button and enter: Bearer <your-token>
                                6. Now you can access protected endpoints

                                ### Option 2: Password-Based Authentication (Admin & Shop Owner)
                                1. Call POST /api/v1/auth/login with email and password
                                2. Copy the JWT token from the response
                                3. Click 'Authorize' button and enter: Bearer <your-token>
                                4. Now you can access protected endpoints

                                ## Features
                                - OTP-based authentication via email
                                - Password-based authentication for admins and shop owners
                                - Shop management (create, update, configure)
                                - Real-time queue management
                                - Redis-backed queue state
                                - Circuit breaker for email service
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Smart Queue Team")
                                .email("support@smartqueue.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.smartqueue.com")
                                .description("Production Server (not yet deployed)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
