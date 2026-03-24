# Hands-on Microservices with Spring Boot Workshop

**Author:** [Simon Martinelli](https://martinelli.ch)
**Duration:** 3 hours
**Versions:** Spring Boot 4.0.x and Spring Cloud 2025.1.x

## Overview

Build and deploy production-grade microservices-based applications with Spring Boot, Java, and Spring Cloud.

Microservices promise scalability and flexibility, but they also introduce complexity, failure modes, and operational
challenges. This workshop cuts through the hype and focuses on what actually matters when building microservices with
Spring Boot.

Instead of abstract diagrams or over-engineered demos, you'll build a small but realistic microservices system from
scratch, step by step. You'll work with multiple services, service discovery, an API gateway, centralized configuration,
resilience, and observability using Spring Boot and Spring Cloud.

The goal isn't to cover everything. It's to help you understand the core building blocks, the trade-offs behind them,
and how they fit together in real systems, so that you can make better architectural decisions.

## Architecture

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│              │     │                  │     │                  │
│  API Gateway ├────►│ catalog-service  │     │  Config Server   │
│  (8080)      ├──┐  │ (8081)           │     │  (8888)          │
│              │  │  │                  │     │                  │
└──────┬───────┘  │  └──────────────────┘     └──────────────────┘
       │          │
       │          │  ┌──────────────────┐
       │          └─►│                  │
       │             │  order-service   ├────► catalog-service
       │             │  (8082)          │     (validates books)
       │             │                  │
       │             └──────────────────┘
       │
       │          ┌──────────────────┐
       └─────────►│                  │
                  │ Discovery Server │
                  │ (Eureka - 8761)  │
                  │                  │
                  └──────────────────┘
```

| Service          | Port | Description               |
|------------------|------|---------------------------|
| discovery-server | 8761 | Eureka service registry   |
| config-server    | 8888 | Centralized configuration |
| api-gateway      | 8080 | Single entry point        |
| catalog-service  | 8081 | Book CRUD (H2 database)   |
| order-service    | 8082 | Order placement (H2)      |

## Who Should Attend?

This workshop is ideal for:

- Java developers with basic Spring Boot experience
- Developers moving from monoliths to microservices
- Architects evaluating Spring Cloud in practice
- Engineering teams that want hands-on microservices experience
- Anyone who wants to understand microservices trade-offs, not just tools

Basic Spring Boot knowledge is required.

## What You'll Learn

- End-to-end, hands-on microservices build with Spring Boot
- A realistic reference architecture you can reuse
- Clear mental models for microservices fundamentals
- Practical experience with Spring Cloud components
- Resilience patterns applied and tested hands-on
- Observability basics with Actuator and Micrometer
- Docker Compose setup to run the full system locally
- Open discussions & Q&A grounded in real-world trade-offs

## Learning Outcomes

By the end of the workshop, you'll be able to:

- Design clear service boundaries and understand data ownership
- Build and run multiple Spring Boot microservices locally
- Use service discovery and route traffic via an API gateway
- Centralize configuration using Spring Cloud Config
- Apply basic resilience patterns and handle failures gracefully
- Add logs, metrics, and health checks for observability
- Understand when microservices make sense — and when they don't
- Know what to learn next

## Prerequisites

- **Java 25** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **Docker Desktop** (`docker info`)
- **Git** (`git --version`)
- An IDE (IntelliJ IDEA recommended) or a text editor + terminal

## Getting Started

```bash
# Clone the repository
git clone <repo-url>
cd hands-on-microservices

# Verify the build
mvn clean compile

# Run a service to verify your setup
mvn -pl catalog-service spring-boot:run
```

Then follow the **[Workshop Guide](workshop-guide.md)** for step-by-step exercises.

## Workshop Sections

| Section | Topic                              | Duration |
|---------|------------------------------------|----------|
| 0       | Setup Verification                 | 10 min   |
| 1       | Microservices Mental Model         | 15 min   |
| 2       | Building the Core Services         | 35 min   |
|         | *Break*                            | 10 min   |
| 3       | Service Discovery & API Gateway    | 30 min   |
| 4       | Centralized Configuration          | 15 min   |
| 5       | Resilience Patterns                | 25 min   |
| 6       | Observability                      | 15 min   |
| 7       | Docker Compose & Full System       | 10 min   |
| 8       | When (Not) to Use Microservices    | 15 min   |

## Tech Stack

- **Spring Boot 4.0.4** / **Spring Cloud 2025.1.1**
- Spring Cloud Netflix Eureka (service discovery)
- Spring Cloud Gateway Server MVC (API gateway)
- Spring Cloud Config (centralized configuration)
- Spring Framework Resilience — built-in `@Retryable` (retry)
- Spring Boot Actuator & Micrometer (observability)
- H2 in-memory database (per service)
- Docker Compose (local deployment)

## Running the Full System with Docker

```bash
mvn clean package -DskipTests
docker compose up --build
```

All traffic goes through the API Gateway at http://localhost:8080.
