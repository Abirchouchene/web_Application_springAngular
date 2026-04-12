# Web Application - Spring Boot & Angular

Full-stack web application built with **Spring Boot** microservices and **Angular 19** frontend.

## Architecture

| Service | Port | Description |
|---------|------|-------------|
| **eureka-server** | 8761 | Service Discovery (Eureka) |
| **config-server** | - | Centralized Configuration |
| **api-gateway** | 9090 | API Gateway (Spring Cloud Gateway) |
| **callcenter-service** | 8082 | Core Call Center Business Logic |
| **contact-service** | 8081 | Contact Management |
| **frontend** | 4200 | Angular 19 SPA |

## Tech Stack

### Backend
- Java 17 / Spring Boot 3.2.3
- Spring Cloud Gateway
- Spring Security + Keycloak (OAuth2/OIDC)
- Spring Data JPA / MySQL
- WebSocket (STOMP over SockJS)
- OpenPDF for report generation

### Frontend
- Angular 19
- Angular Material
- ApexCharts (Dashboard)
- Tabler Icons
- SockJS + STOMP (Real-time notifications)

## Features
- Role-based access (Admin, Manager, Agent, Demandeur)
- Real-time dashboard with KPIs and AI insights
- Request & ticket management
- Agent assignment and workload tracking
- PDF report generation
- WebSocket real-time notifications
- AI Chat assistant
- Contact management (CRUD)
- Calls management interface

## Getting Started

1. Start **eureka-server**
2. Start **config-server**
3. Start **contact-service** and **callcenter-service**
4. Start **api-gateway**
5. Start **frontend** (`npm install && ng serve`)

Access the app at `http://localhost:4200`
