# CallFlow — Call Center Management Platform

> A full-stack, production-grade call center management platform built with **Spring Boot 3** microservices, **Angular 19**, and **Keycloak** authentication. Designed for managing survey requests, agent workflows, contact campaigns, and automated reporting with AI-powered insights.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)

---

## Architecture

The platform follows a **microservices architecture** with service discovery, centralized configuration, and an API gateway:

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐
│   Frontend   │────▶│  API Gateway  │────▶│  Eureka Discovery │
│  Angular 19  │     │   Port 9090   │     │    Port 8761      │
└─────────────┘     └──────┬───────┘     └───────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼                         ▼
   ┌──────────────────┐     ┌──────────────────┐
   │ CallCenter Svc   │     │  Contact Svc     │
   │   Port 8082      │     │   Port 8081      │
   │   (Core Logic)   │     │  (Contacts/Tags) │
   └────────┬─────────┘     └──────────────────┘
            │
   ┌────────┼────────┬──────────┐
   ▼        ▼        ▼          ▼
 MySQL   Keycloak   MinIO    OpenAI
```

| Service | Port | Description |
|---------|------|-------------|
| **eureka-server** | 8761 | Service Discovery (Netflix Eureka) |
| **config-server** | — | Centralized Configuration (Spring Cloud Config) |
| **api-gateway** | 9090 | API Gateway with load balancing (Spring Cloud Gateway) |
| **callcenter-service** | 8082 | Core business logic: requests, reports, notifications, AI |
| **contact-service** | 8081 | Contact management, callbacks, tags |
| **frontend** | 4200 | Angular 19 SPA with Material Design |

## Tech Stack

### Backend
- **Java 17** / **Spring Boot 3.2.3**
- **Spring Cloud** — Gateway, Eureka, Config Server, OpenFeign
- **Spring Security** + **Keycloak** (OAuth2 / OIDC)
- **Spring Data JPA** / **MySQL**
- **WebSocket** (STOMP over SockJS) for real-time notifications
- **MinIO** — S3-compatible object storage for PDF reports
- **OpenPDF** — PDF report generation
- **OpenAI GPT-4** — AI insights and chat assistant
- **Mailjet** — Transactional emails (approval notifications, password reset)
- **RabbitMQ** — Event-driven messaging

### Frontend
- **Angular 19** (Standalone components)
- **Angular Material** — UI component library
- **ApexCharts** — Interactive dashboard charts
- **Keycloak Angular** — SSO authentication
- **SockJS + STOMP** — Real-time WebSocket notifications
- **ngx-translate** — Internationalization

## Features

### 🎯 Core
- **Role-based access control** — Admin, Manager, Agent, Survey Requester
- **Request lifecycle** — Create → Approve → Assign → In Progress → Resolved → Report
- **Agent assignment** with workload tracking and availability management

### 📊 Dashboard & Analytics
- Real-time KPI cards (total requests, contact rates, agent performance)
- Interactive charts with ApexCharts
- AI-powered insights and recommendations (OpenAI GPT-4)

### 📝 Reporting
- Automated report generation with scheduled cron jobs
- PDF export with statistics, question summaries, and AI analysis
- **MinIO storage** for persistent PDF access with presigned URLs
- Local file fallback when MinIO is unavailable
- Email delivery of approved reports

### 🔔 Notifications
- Real-time WebSocket push notifications
- Callback reminders and scheduling
- Notification deduplication

### 📞 Contact Management
- Full CRUD for contacts with tags
- Callback scheduling and tracking (3-tab view: Upcoming / Completed / All)
- Contact status tracking per request

### 🤖 AI Features
- AI Chat assistant for agents
- Automated report insights generation
- Dashboard AI analysis

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0+
- Keycloak 22+ (configured at `http://localhost:8080`)
- MinIO (optional, for PDF storage)

### Launch Order

```bash
# 1. Service Discovery
cd eureka-server && mvn spring-boot:run

# 2. Configuration Server
cd config-server && mvn spring-boot:run

# 3. Microservices
cd contact-service && mvn spring-boot:run
cd callcenter-service && mvn spring-boot:run

# 4. API Gateway
cd api-gateway && mvn spring-boot:run

# 5. Frontend
cd frontend && npm install && ng serve
```

Access the app at **http://localhost:4200**

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

Built with ❤️ by [Abir Chouchene](https://github.com/Abirchouchene)
