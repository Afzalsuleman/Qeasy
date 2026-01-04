# Smart Queue Management System (Qeasy)

A modern, real-time queue management system built with Spring Boot 3, designed to streamline customer flow for shops and service centers. Features OTP-based authentication, WebSocket real-time updates, and comprehensive analytics.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

## 🚀 Features

### Core Functionality
- **OTP-Based Authentication**: Secure, passwordless authentication via email
- **Queue Management**: Join, leave, and track position in real-time queues
- **Shop Management**: CRUD operations for shop owners
- **Real-time Updates**: WebSocket-based live queue notifications
- **Analytics Dashboard**: Comprehensive metrics and statistics

### Advanced Features
- **Atomic Queue Operations**: Redis-backed Lua scripts for thread-safe operations
- **Auto NO_SHOW Detection**: Automatic detection and marking of unresponsive users
- **Email Retry Mechanism**: Exponential backoff retry for failed emails
- **Distributed Locking**: ShedLock integration for multi-instance deployments
- **Circuit Breaker**: Resilience4j for fault-tolerant email service
- **API Documentation**: Interactive Swagger UI with JWT authentication

## 🛠️ Tech Stack

### Backend Framework
- **Spring Boot 3.2.1** - Application framework
- **Java 17** - Programming language
- **Gradle 8.5** - Build automation

### Data & Caching
- **PostgreSQL 15** - Primary database
- **Redis 7** - Caching and queue state management
- **Flyway 10.4.1** - Database migration
- **Spring Data JPA** - Data persistence
- **HikariCP** - Connection pooling

### Security & Authentication
- **Spring Security** - Security framework
- **JWT (JJWT 0.12.3)** - Token-based authentication
- **OTP Authentication** - Email-based verification

### Real-time Communication
- **WebSocket (STOMP)** - Real-time bidirectional communication
- **SimpMessagingTemplate** - Message broadcasting

### Resilience & Reliability
- **Resilience4j 2.1.0** - Circuit breaker and retry patterns
- **ShedLock 5.10.0** - Distributed scheduler locking

### Email & Notifications
- **Spring Mail** - Email service
- **Gmail SMTP** - Email delivery
- **Thymeleaf** - HTML email templates

### API & Documentation
- **Springdoc OpenAPI 3.0** - API documentation
- **Swagger UI 2.2.0** - Interactive API explorer

### Monitoring & Observability
- **Spring Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Metrics export
- **SLF4J + Logback** - Logging
- **MDC Correlation IDs** - Request tracing

### Testing (Planned - Phase 4)
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Testcontainers 1.19.3** - Integration testing
- **GreenMail 2.0.1** - Email testing

## 📋 Prerequisites

- Java 17 or higher
- Docker & Docker Compose
- Gradle 8.5+ (or use included wrapper)
- PostgreSQL 15 (via Docker)
- Redis 7 (via Docker)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Qeasy
```

### 2. Configure Environment Variables
Create a `.env` file in the project root:
```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=smart_queue
DB_USERNAME=sq_user
DB_PASSWORD=change_me_in_production

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Configuration (Generate with: openssl rand -base64 32)
JWT_SECRET=your-256-bit-secret-key-change-in-production

# Gmail SMTP Configuration
GMAIL_USERNAME=your-email@gmail.com
GMAIL_PASSWORD=your-app-password

# Application Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

### 3. Start Infrastructure Services
```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379

### 4. Build the Application
```bash
./gradlew clean build -x test
```

### 5. Run the Application
```bash
./gradlew bootRun
```

The application will be available at:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## 📚 API Endpoints

### Authentication (Public)
```
POST   /api/v1/auth/generate-otp    Generate OTP and send via email
POST   /api/v1/auth/verify-otp      Verify OTP and receive JWT token
GET    /api/v1/auth/health          Health check endpoint
```

### Shop Management (Authenticated)
```
POST   /api/v1/shops                Create a new shop
GET    /api/v1/shops/{id}           Get shop by ID
GET    /api/v1/shops                List all active shops
GET    /api/v1/shops/my-shop        Get authenticated user's shop
PUT    /api/v1/shops/{id}           Update shop details
DELETE /api/v1/shops/{id}           Deactivate shop (soft delete)
```

### Queue Operations (Authenticated)
```
POST   /api/v1/queue/join           Join a shop's queue
DELETE /api/v1/queue/leave/{shopId} Leave a queue
POST   /api/v1/queue/call-next/{shopId}  Call next user (owner only)
GET    /api/v1/queue/position/{shopId}   Get current position in queue
```

### Analytics (Authenticated)
```
GET    /api/v1/analytics/shop/{shopId}         Get comprehensive analytics
GET    /api/v1/analytics/shop/{shopId}/current Get real-time queue stats
GET    /api/v1/analytics/shop/{shopId}/today   Get today's statistics
```

### WebSocket Endpoints
```
CONNECT /ws                          WebSocket connection endpoint
SUBSCRIBE /topic/queue/{shopId}      Subscribe to queue updates
SUBSCRIBE /queue/notifications       Subscribe to personal notifications
```

## 🔐 Authentication Flow

1. **Generate OTP**: POST to `/api/v1/auth/generate-otp` with email and name
2. **Check Email**: Receive 6-digit OTP (valid for 5 minutes)
3. **Verify OTP**: POST to `/api/v1/auth/verify-otp` with email and OTP
4. **Receive JWT**: Get JWT token (valid for 24 hours)
5. **Use Token**: Add `Authorization: Bearer {token}` header to requests

## 🏗️ Architecture

### Project Structure
```
smart-queue/
├── src/main/java/com/smartqueue/
│   ├── config/              # Configuration classes
│   │   ├── OpenApiConfig.java
│   │   ├── RedisConfig.java
│   │   ├── SchedulerConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/          # REST controllers
│   │   ├── AnalyticsController.java
│   │   ├── AuthController.java
│   │   ├── QueueController.java
│   │   └── ShopController.java
│   ├── service/             # Business logic
│   │   ├── AnalyticsService.java
│   │   ├── AuthService.java
│   │   ├── EmailService.java
│   │   ├── QueueService.java
│   │   ├── ShopService.java
│   │   └── WebSocketService.java
│   ├── scheduler/           # Background jobs
│   │   ├── FailedEmailRetryScheduler.java
│   │   ├── QueueLogCleanupScheduler.java
│   │   └── StaleQueueCleanupScheduler.java
│   ├── repository/          # Data access
│   ├── model/               # Entities, DTOs, Enums
│   ├── security/            # Security components
│   ├── filter/              # Request filters
│   └── exception/           # Exception handlers
├── src/main/resources/
│   ├── db/migration/        # Flyway migrations
│   ├── lua/                 # Redis Lua scripts
│   │   ├── join_queue.lua
│   │   ├── leave_queue.lua
│   │   └── call_next_user.lua
│   ├── templates/           # Email templates
│   └── application.yml      # Application configuration
└── src/test/                # Test files (Phase 4)
```

### Redis Data Structures
```
queue:{shopId}                 - Sorted set (queue positions)
queue:{shopId}:users           - Hash (user details)
queue:{shopId}:waiting         - Set (waiting users)
queue:{shopId}:current         - String (current user being served)
otp:{email}                    - String (OTP code, 5 min TTL)
email:daily:{date}             - Counter (daily email limit)
shedlock:*                     - Distributed locks
```

### Database Schema
- **users** - User accounts
- **shops** - Shop information
- **queue_logs** - Queue activity audit trail (30-day retention)
- **failed_emails** - Failed email retry queue

## 🔄 Background Jobs

### QueueLogCleanupScheduler
- **Schedule**: Daily at 2:00 AM
- **Purpose**: Delete queue logs older than 30 days
- **Locking**: ShedLock (10 min max, 5 min min)

### FailedEmailRetryScheduler
- **Schedule**: Every 5 minutes
- **Purpose**: Retry failed emails with exponential backoff
- **Max Attempts**: 5
- **Backoff**: 2^n minutes (2, 4, 8, 16, 32 minutes)

### StaleQueueCleanupScheduler
- **Schedule**: Every hour
- **Purpose**: Auto-mark unresponsive users as NO_SHOW
- **Timeout**: 15 minutes after being called

## 📊 Monitoring & Health

### Actuator Endpoints
```
GET /actuator/health         Application health status
GET /actuator/info           Application information
GET /actuator/metrics        Application metrics
GET /actuator/prometheus     Prometheus metrics
```

### Logging
- **Correlation IDs**: Every request has a unique correlation ID
- **MDC**: Mapped Diagnostic Context for distributed tracing
- **Log Levels**: Configurable via application.yml

## 🧪 Testing (Phase 4 - Planned)

### Test Commands
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests AuthServiceTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Coverage Goals
- **Unit Tests**: 85%+ code coverage
- **Integration Tests**: Critical flows
- **Controller Tests**: All endpoints

## 🚢 Deployment

### Production Checklist
- [ ] Update `JWT_SECRET` with strong 256-bit key
- [ ] Change `DB_PASSWORD` from default
- [ ] Configure production email credentials
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS allowed origins
- [ ] Set up monitoring and alerting
- [ ] Configure log aggregation
- [ ] Set up database backups
- [ ] Configure Redis persistence

### Docker Deployment
```bash
# Build application
./gradlew bootJar

# Run with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f smart-queue-app
```

## 📈 Performance

### Optimizations
- **Connection Pooling**: HikariCP for database connections
- **Redis Caching**: Queue state cached in Redis
- **Lua Scripts**: Atomic operations reduce round-trips
- **Async Email**: Non-blocking email sending
- **Database Indexes**: Optimized query performance

### Scalability
- **Stateless Design**: Horizontal scaling supported
- **Distributed Locking**: ShedLock prevents duplicate scheduler runs
- **Redis Shared State**: Queue state shared across instances
- **Load Balancer Ready**: Sticky sessions not required

## 🛡️ Security Features

- **JWT Authentication**: Secure token-based auth
- **Password-less**: OTP-based authentication
- **CORS**: Configurable cross-origin policies
- **Input Validation**: Jakarta Bean Validation
- **SQL Injection Prevention**: JPA/Hibernate parameterized queries
- **Rate Limiting**: OTP generation limited to 3/5 minutes
- **Email Rate Limiting**: 100 emails/day per account

## 📝 Development

### Database Management
```bash
# Connect to PostgreSQL
docker exec -it smart-queue-postgres psql -U sq_user -d smart_queue

# Connect to Redis
docker exec -it smart-queue-redis redis-cli

# Reset database
docker-compose down
docker volume rm qeasy_postgres_data
docker-compose up -d
```

### Code Style
- **Java**: Follow Google Java Style Guide
- **Naming**: Use meaningful, descriptive names
- **Comments**: Javadoc for public methods
- **Lombok**: Use for boilerplate reduction

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Afzal Sulemani** - *Initial work* - [afzalsulemani9939@gmail.com](mailto:afzalsulemani9939@gmail.com)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Redis team for high-performance caching
- PostgreSQL team for reliable database
- Claude Code for development assistance

## 📞 Support

For support, email afzalsulemani9939@gmail.com or open an issue in the repository.

---

**Built with ❤️ using Spring Boot 3**
