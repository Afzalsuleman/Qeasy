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
- POST /auth/otp/request
- POST /auth/otp/verify

Notes:
- OTP stored in Redis (TTL 5 min)
- JWT expiry 24 hrs

---

## 4.2 Shop Module
APIs:
- POST /shops
- GET /shops/{shopId}
- PATCH /shops/{shopId}/config

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
created_at TIMESTAMP  

### shops
id UUID PK  
name VARCHAR  
lat DOUBLE  
lng DOUBLE  
avg_service_time INT  
max_queue_size INT  

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

## 8. Cursor / Claude Prompt

Build a Spring Boot 3 backend with Redis-backed queue management, WebSockets for real-time updates, and Dockerized PostgreSQL & Redis.

---

## ✅ Backend Ready
