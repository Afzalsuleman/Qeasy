# 📕 Functional Scope Document (FSD)
## Smart Queue – Backend Services

---

## 1. Scope & Responsibility

### In Scope
- OTP-based authentication
- Shop & owner management
- Queue lifecycle management
- Real-time queue updates (WebSockets)
- Auto-cancel & no-show handling
- Notification trigger hooks
- Observability & health checks

### Out of Scope (MVP)
- Payments
- Food ordering
- Loyalty programs
- Mobile applications
- Super admin panel

---

## 2. Service Architecture

### Monolith (MVP Phase)
```
smart-queue-backend
├── auth
├── shop
├── queue
├── websocket
├── notification
├── scheduler
└── common
```

---

## 3. Technology Stack (Backend)

### Core
- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Security (JWT)
- Spring WebSocket (STOMP)
- Spring Data JPA

### Infrastructure (Dockerized)
- PostgreSQL 15
- Redis 7
- Docker & Docker Compose
- Actuator (metrics & health)

---

## 4. Functional Modules

## 4.1 Authentication Module
APIs:
- POST /api/v1/auth/request-otp
- POST /api/v1/auth/verify-otp

Features:
- OTP stored in Redis (TTL 5 min)
- JWT expiry 24 hrs
- Role-based user promotion (USER → SHOP_OWNER when creating shop)

Notes:
- OTP emails are NOT auto-retried (OTP expires in 5 minutes)
- Other email types (NOTIFICATION, QUEUE_UPDATE, SYSTEM_ALERT) use exponential backoff retry (up to 5 attempts)

---

## 4.2 Shop Module
APIs:
- POST /api/v1/shops (requires SHOP_OWNER role)
- GET /api/v1/shops (public - browse all active shops)
- GET /api/v1/shops/{shopId} (public - view shop details)
- PUT /api/v1/shops/{shopId} (requires SHOP_OWNER role - shop owner only)
- DELETE /api/v1/shops/{shopId} (requires SHOP_OWNER role - soft delete)
- GET /api/v1/shops/owner/me (requires authentication - get owner's shop)

Features:
- Shop branding with image URLs
- Queue capacity management
- Average service time configuration
- Soft delete (isActive flag)
- Current queue size and wait time estimation

---

## 4.3 Queue Module (Core)

Redis:
- queue:{shopId} → Sorted Set (timestamp, userId)
- user:queue:{userId} → shopId (TTL 2 hrs)

APIs:
- POST /queue/{shopId}/join
- POST /queue/{shopId}/leave
- GET /queue/{shopId}/status
- POST /queue/{shopId}/next

---

## 4.4 WebSocket Module
Endpoint:
- /ws

Topic:
- /topic/queue/{shopId}

---

## 5. Database Schema

### users
id UUID PK
phone VARCHAR UNIQUE
email VARCHAR UNIQUE
role VARCHAR (USER, SHOP_OWNER) - Default: USER
created_at TIMESTAMP
updated_at TIMESTAMP

### shops
id UUID PK
owner_id UUID FK (users.id)
name VARCHAR
description VARCHAR
address VARCHAR
image_url VARCHAR (500) - Shop branding image
avg_service_time_minutes INT
max_queue_size INT
is_active BOOLEAN - Soft delete flag
created_at TIMESTAMP
updated_at TIMESTAMP

### failed_emails
id BIGSERIAL PK
recipient VARCHAR
subject VARCHAR
body TEXT
email_body TEXT - Stores actual OTP or message content
email_type VARCHAR (OTP, NOTIFICATION, QUEUE_UPDATE, SYSTEM_ALERT)
status VARCHAR (PENDING, SENT, FAILED)
attempt_count INT
retry_count INT
failed_at TIMESTAMP
retry_after TIMESTAMP
failure_reason TEXT
created_at TIMESTAMP

Note: OTP emails are never auto-retried. Other email types use exponential backoff (2, 4, 8, 16, 32 minutes, max 5 attempts).

### queue_logs
id BIGSERIAL PK
shop_id UUID
user_id UUID
status VARCHAR

---

## 6. Docker Setup

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    environment:
      DB_HOST: postgres
      REDIS_HOST: redis

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: smart_queue
      POSTGRES_USER: sq_user
      POSTGRES_PASSWORD: sq_pass

  redis:
    image: redis:7
```

---

## 7. Non-Functional Requirements

- Latency < 200ms
- Atomic queue operations
- Redis as source of truth
- Horizontal scalability

---

## 8. Security Architecture

### Authentication & Authorization
- **JWT-based Authentication**: Stateless token-based auth with 24-hour expiry
- **Three-Tier Role-Based Access Control (RBAC)**:
  - **USER**: Default role for regular authenticated users (can join queues, browse shops)
  - **SHOP_OWNER**: Created by admin, can manage their shop and view analytics
  - **ADMIN**: Can create shop owners, view all shops, and access system-wide analytics
  - Auto-promotion: USER → SHOP_OWNER when creating first shop

### Endpoint Authorization (Spring Security)
```
Public Endpoints (No Auth Required):
- POST /api/v1/auth/generate-otp
- POST /api/v1/auth/verify-otp
- GET /api/v1/shops (list all active shops)
- GET /api/v1/shops/{shopId} (view shop details)
- /ws/** (WebSocket, authenticated via interceptor)

Protected Endpoints (Authenticated Users):
- GET /api/v1/shops/owner/me (get owner's shop)
- POST /api/v1/auth/change-password (set password after invitation)

SHOP_OWNER Only:
- POST /api/v1/shops (create shop)
- PUT /api/v1/shops/{shopId} (update shop)
- DELETE /api/v1/shops/{shopId} (delete shop)
- GET /api/v1/analytics/** (shop analytics)

ADMIN Only:
- POST /api/v1/admin/shop-owners (create shop owner)
- GET /api/v1/admin/shop-owners (list all shop owners)
- GET /api/v1/admin/dashboard (system dashboard)
```

### Email Security & Retry Strategy
- **OTP Emails**: NO auto-retry (OTP expires in 5 min, user must request new)
- **Shop Owner Invitations**: Exponential backoff retry (2, 4, 8, 16, 32 min, max 5 attempts)
- **Other Emails**: Exponential backoff retry for notifications and alerts
- **Failed Email Tracking**: emailType enum distinguishes categories for appropriate handling

---

## 9. Completed Features (Current Session)

### ✅ Three-Tier User System (Admin, Shop Owner, Regular User)
- **UserRole enum**: ADMIN, SHOP_OWNER, USER with clear responsibilities
- **Admin Capabilities**:
  - Create shop owners via email invitation (POST /api/v1/admin/shop-owners)
  - List all shop owners in the system (GET /api/v1/admin/shop-owners)
  - View system-wide dashboard with shop counts and metrics (GET /api/v1/admin/dashboard)
  - Access analytics for all shops (admin can see all shop data)
- **Shop Owner Workflow**:
  1. Admin creates shop owner account with email
  2. Shop owner receives invitation email with login instructions
  3. Shop owner logs in via OTP and sets password (POST /api/v1/auth/change-password)
  4. After password set, shop owner can create/manage shops and view analytics
- **User Promotion**: Regular users auto-promoted from USER → SHOP_OWNER when creating their first shop
- **Password Management**:
  - Passwords ONLY required for ADMIN and SHOP_OWNER roles
  - Regular USER roles do NOT have password protection (auth via OTP only)
  - passwordSet flag tracks initial setup for admins/shop owners
  - Secure Bcrypt encoding for password storage
  - Attempting to set password as regular USER returns error

### ✅ Role-Based Access Control with Three Tiers
- Implemented Spring Security's native `hasAnyRole()` authorization
- Centralized in SecurityConfig.java with three role levels
- ADMIN-only endpoints: /api/v1/admin/**
- SHOP_OWNER-only: Shop management + analytics (/api/v1/analytics/**)
- Automatic user promotion: USER → SHOP_OWNER when creating shop
- All authorization decisions at request boundary, no AOP complexity

### ✅ OTP Email Retry Fix
- **Problem**: Users receiving multiple emails without OTP code
- **Root Cause**: Missing failedAt timestamp, no emailType distinction
- **Solution**:
  - Added emailType field to FailedEmail entity
  - Skip OTP emails in retry scheduler (they expire in 5 minutes)
  - Fixed OTP extraction from stored email body
  - Proper timestamp handling in saveFailedEmail()
- **Result**: OTP emails never auto-retry, users must request new OTP

### ✅ Public Shop Browsing
- GET /api/v1/shops - Public endpoint (users can browse all active shops)
- GET /api/v1/shops/{shopId} - Public endpoint (users can view shop details)
- Includes current queue size and estimated wait time
- Allows users to discover shops without authentication

### ✅ Shop Image Support
- Added imageUrl field to Shop entity (VARCHAR 500)
- Support for image URLs in shop creation (CreateShopRequest)
- Support for image URLs in shop updates (UpdateShopRequest)
- Included in API responses (ShopResponse)
- Database migration V10 created with proper indexing

### ✅ Database Migrations Created
- **V8**: Add user_role column with constraints and indexing (USER, SHOP_OWNER, ADMIN roles)
- **V9**: Add email_type column with constraints and indexing (OTP, NOTIFICATION, QUEUE_UPDATE, SYSTEM_ALERT)
- **V10**: Add image_url column to shops table (VARCHAR 500 for shop branding)
- **V11**: Add password and passwordSet columns to users table for shop owner authentication

### ✅ Application Status
- ✅ All tests pass
- ✅ Clean build successful
- ✅ No compilation errors
- ✅ Application starts without errors

---

## 8. Cursor / Claude Prompt

Build a Spring Boot 3 backend with Redis-backed queue management, WebSockets for real-time updates, and Dockerized PostgreSQL & Redis.

---

## ✅ Backend Ready - Security & Shop Features Complete
