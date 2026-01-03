# Smart Queue Management System

A production-ready backend service that enables shops to manage customer queues digitally, reducing physical wait times and improving customer experience.

## Version

**v1.0.0** - MVP Phase (Development In Progress)

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Gradle 8.5
- **Database**: PostgreSQL 15
- **Cache/Queue**: Redis 7
- **Migration**: Flyway
- **Security**: Spring Security + JWT
- **Email**: Gmail SMTP with Resilience4j Circuit Breaker
- **WebSocket**: STOMP over WebSocket
- **Monitoring**: Spring Boot Actuator + Prometheus
- **Documentation**: SpringDoc OpenAPI 3.0
- **Testing**: JUnit 5, Mockito, TestContainers

## Key Features

✅ **Implemented:**
- ✅ Project structure with Gradle
- ✅ Spring Boot application configuration
- ✅ Domain entities (User, Shop, QueueLog, FailedEmail)
- ✅ Enums (QueueStatus, EmailStatus, ErrorCode)
- ✅ Flyway migrations (V1-V6) with auto-update triggers
- ✅ Comprehensive application.yml with profiles (dev, test, prod)
- ✅ Circuit breaker & retry configuration (Resilience4j)

🚧 **In Progress:**
- JWT authentication and OTP service
- Email service with circuit breaker
- Redis-based queue management with Lua scripts
- WebSocket real-time updates
- Scheduler services (no-show detection, cleanup)

📋 **Pending:**
- REST controllers
- Global exception handler
- Correlation ID filter
- Service layer implementations
- Unit and integration tests
- Docker Compose configuration
- OpenAPI documentation

## Project Structure

```
smart-queue/
├── src/
│   ├── main/
│   │   ├── java/com/smartqueue/
│   │   │   ├── SmartQueueApplication.java
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── filter/              # Filters (CORS, Correlation ID)
│   │   │   ├── model/
│   │   │   │   ├── dto/            # Request/Response DTOs
│   │   │   │   ├── entity/         # JPA entities ✅
│   │   │   │   └── enums/          # Enums ✅
│   │   │   ├── repository/         # JPA repositories
│   │   │   ├── scheduler/          # Scheduled tasks
│   │   │   ├── security/           # Security components
│   │   │   ├── service/            # Business logic
│   │   │   └── util/               # Utility classes
│   │   └── resources/
│   │       ├── application.yml      ✅
│   │       ├── db/migration/        # Flyway migrations ✅
│   │       ├── lua/                 # Lua scripts for Redis
│   │       └── templates/email/     # Thymeleaf email templates
│   └── test/
│       ├── java/com/smartqueue/
│       │   ├── service/            # Unit tests
│       │   ├── controller/         # Controller tests
│       │   ├── repository/         # Repository tests
│       │   └── integration/        # Integration tests
│       └── resources/
│           └── application-test.yml
├── build.gradle                     ✅
├── settings.gradle                  ✅
├── gradlew                          ✅
└── docker-compose.yml               ⏳