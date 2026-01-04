# Smart Queue Management System

Backend service for digital queue management in shops.

## 📊 Project Status

**Phase 1: Foundation** - ✅ **100% COMPLETE**
**Overall Progress**: 25% complete (31 of 160 hours)

👉 **See [DEVELOPMENT_TRACKER.md](DEVELOPMENT_TRACKER.md) for detailed progress**

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose

### Setup

```bash
# 1. Start services
docker-compose up -d

# 2. Run migrations
./gradlew flywayMigrate

# 3. Build project
./gradlew clean build -x test

# 4. Run application (when services are implemented)
./gradlew bootRun
```

---

## 🛠️ Tech Stack

- **Java 17** with Spring Boot 3.2.1
- **PostgreSQL 15** for persistence
- **Redis 7** for queue state
- **Gradle 8.5** for build
- **JWT** for authentication
- **WebSocket** for real-time updates
- **Flyway** for database migrations
- **Resilience4j** for circuit breaker

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| **[DEVELOPMENT_TRACKER.md](DEVELOPMENT_TRACKER.md)** | 👈 **MAIN TRACKING DOCUMENT** |
| [tech-design-v3.md](tech-design-v3.md) | Technical specifications |
| [BUILD_STATUS.md](BUILD_STATUS.md) | Build info & troubleshooting |
| [docker-compose.yml](docker-compose.yml) | Infrastructure setup |

---

## 📝 What's Implemented (Phase 1)

✅ Project setup with Gradle
✅ Domain entities (User, Shop, QueueLog, FailedEmail)
✅ Database migrations (6 Flyway scripts)
✅ Exception framework (12 custom exceptions)
✅ Error handling (GlobalExceptionHandler)
✅ Repositories (4 JPA repositories)
✅ Security (JWT authentication)
✅ Correlation ID filter
✅ Docker Compose setup

**Total**: 49 files created

---

## 🚧 What's Next (Phase 2)

🔴 AuthService (OTP + JWT)
🔴 EmailService (Gmail SMTP + Circuit Breaker)
🔴 ShopService (CRUD operations)
🔴 QueueService (Redis + Lua scripts)

**Estimated**: 40 hours

---

## 📚 Documentation

- **Main Tracker**: [DEVELOPMENT_TRACKER.md](DEVELOPMENT_TRACKER.md) ← Start here
- **Tech Design**: [tech-design-v3.md](tech-design-v3.md)
- **Tech Review**: [tech-design-review-v3.md](tech-design-review-v3.md)

---

## 🔧 Commands

```bash
# Build
./gradlew clean build

# Test (when implemented)
./gradlew test

# Run
./gradlew bootRun

# Docker
docker-compose up -d    # Start services
docker-compose ps       # Check status
docker-compose down     # Stop services
```

---

## 📞 Getting Help

1. Read [DEVELOPMENT_TRACKER.md](DEVELOPMENT_TRACKER.md)
2. Check [BUILD_STATUS.md](BUILD_STATUS.md) for build issues
3. Refer to [tech-design-v3.md](tech-design-v3.md) for specs

---

**Build Status**: ✅ PASSING
**Last Updated**: 2026-01-04
