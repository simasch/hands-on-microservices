# Hands-on Microservices with Spring Boot — Workshop Guide

## Prerequisites

Before the workshop, make sure you have:

- **Java 25** installed (`java -version`)
- **Maven 3.9+** installed (`mvn -version`)
- **Docker Desktop** running (`docker info`)
- **Git** installed (`git --version`)
- An IDE (IntelliJ IDEA recommended) or a text editor + terminal
- This repository cloned and opened in your IDE

## Architecture Overview

We're building an **Online Bookstore** with these services:

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│              │     │                  │     │                  │
│  API Gateway ├────►│ catalog-service  │     │  Config Server   │
│  (8080)      ├──┐  │ (8081)           │     │  (8888)          │
│              │  │  │                  │     │                  │
└──────┬───────┘  │  └──────────────────┘     └──────────────────┘
       │          │
       │          │  ┌──────────────────┐     ┌──────────────────┐
       │          └─►│                  │     │                  │
       │             │  order-service   ├────►│ catalog-service  │
       │             │  (8082)          │     │ (validates books)│
       │             │                  │     │                  │
       │             └──────────────────┘     └──────────────────┘
       │
       │          ┌──────────────────┐
       └─────────►│                  │
                  │ Discovery Server │
                  │ (Eureka - 8761)  │
                  │                  │
                  └──────────────────┘
```

| Service          | Port | Description                   |
|------------------|------|-------------------------------|
| discovery-server | 8761 | Eureka service registry       |
| config-server    | 8888 | Centralized configuration     |
| api-gateway      | 8080 | Single entry point            |
| catalog-service  | 8081 | Book CRUD (H2 database)       |
| order-service    | 8082 | Order placement (H2 database) |

---

## Section 0: Setup Verification (15 min)

Verify your environment by building and running the catalog service:

```bash
# From the project root
mvn clean compile

# Run catalog-service
mvn -pl catalog-service spring-boot:run
```

Open http://localhost:8081/api/books you should get a 404 error because the service is not yet implemented.

Press `Ctrl+C` to stop.

---

## Section 1: Microservices Mental Model (15 min)

_Instructor-led discussion — no coding._

Key takeaways:

- A microservice is an independently deployable service built around a **business capability**
- Each service **owns its data** (catalog-service has its own DB, order-service has its own DB)
- Microservices introduce complexity — only use them when the benefits outweigh the costs
- We're building this as microservices **for learning** — a bookshop this small would be better as a monolith

---

## Section 2: Building the Core Services (35 min)

### Exercise 2A: Catalog REST API (10 min)

Open `catalog-service/src/main/java/com/bookshop/catalog/book/BookController.java`

Complete **TODO 1** and **TODO 2**:

**TODO 1** — Return all books:

```java

@GetMapping
public List<Book> getAllBooks() {
    return repository.findAll();
}
```

**TODO 2** — Return a book by ISBN:

```java

@GetMapping("/{isbn}")
public Book getBookByIsbn(@PathVariable String isbn) {
    return repository.findByIsbn(isbn)
            .orElseThrow(() -> new BookNotFoundException(isbn));
}
```

**Verify:** Run catalog-service and test:

```bash
mvn -pl catalog-service spring-boot:run

# In another terminal:
curl http://localhost:8081/api/books
curl http://localhost:8081/api/books/978-0-13-468599-1
```

### Exercise 2B: Inter-Service Communication (10 min)

Open `order-service/src/main/java/com/bookshop/order/client/BookClient.java`

Complete **TODO 3** — Call catalog-service to get a book:

```java
public Optional<BookResponse> getBookByIsbn(String isbn) {
    try {
        BookResponse book = restClient.get()
                .uri("/api/books/{isbn}", isbn)
                .retrieve()
                .body(BookResponse.class);
        return Optional.ofNullable(book);
    } catch (Exception e) {
        return Optional.empty();
    }
}
```

### Exercise 2C: Order Business Logic (10 min)

Open `order-service/src/main/java/com/bookshop/order/order/OrderService.java`

Complete **TODO 4** — Implement `placeOrder`:

```java
public Order placeOrder(CreateOrderRequest request) {
    List<OrderItem> orderItems = new ArrayList<>();
    for (var item : request.items()) {
        BookResponse book = bookClient.getBookByIsbn(item.isbn())
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + item.isbn()));
        orderItems.add(new OrderItem(book.isbn(), book.title(), item.quantity(), book.price()));
    }
    Order order = new Order(orderItems);
    return orderRepository.save(order);
}
```

### Exercise 2D: Order REST API (5 min)

Open `order-service/src/main/java/com/bookshop/order/order/OrderController.java`

Complete **TODO 5** and **TODO 6**:

```java

@PostMapping
public Order placeOrder(@RequestBody CreateOrderRequest request) {
    return orderService.placeOrder(request);
}

@GetMapping
public List<Order> getAllOrders() {
    return orderService.getAllOrders();
}
```

**Verify:** Run both services and test:

```bash
# Terminal 1
mvn -pl catalog-service spring-boot:run

# Terminal 2
mvn -pl order-service spring-boot:run

# Terminal 3 — Place an order
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 2}]}'

# List orders
curl http://localhost:8082/api/orders
```

> **Discussion point:** Notice the hardcoded `http://localhost:8081` in `BookClient`. What happens if catalog-service
> moves?

---

## BREAK (10 min)

---

## Section 3: Service Discovery & API Gateway (30 min)

### Exercise 3A: Register Services with Eureka (10 min)

First, start the discovery server:

```bash
mvn -pl discovery-server spring-boot:run
```

Open http://localhost:8761 — you should see the Eureka dashboard with no registered services.

Now, edit **both** `catalog-service/src/main/resources/application.properties` and
`order-service/src/main/resources/application.properties`.

Complete **TODO 7** — Add Eureka client config (uncomment/add):

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

Restart both services and check the Eureka dashboard — both should appear.

### Exercise 3B: Use Service Discovery in BookClient (5 min)

Complete **TODO 8** in `BookClient.java`:

1. Change the base URL:

```java
.baseUrl("http://catalog-service")
```

2. Create a new configuration class `order-service/src/main/java/com/bookshop/order/RestClientConfig.java`:

```java
package com.bookshop.order;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
```

### Exercise 3C: API Gateway Routes (15 min)

Open `api-gateway/src/main/java/com/bookshop/gateway/GatewayRouteConfig.java`

Complete **TODO 9**:

```java

@Bean
public RouterFunction<ServerResponse> gatewayRoutes() {
    return route("catalog_route")
            .GET("/api/books/**", http())
            .before(uri("lb://catalog-service"))
            .build()
            .and(
                    route("order_post_route")
                            .POST("/api/orders", http())
                            .before(uri("lb://order-service"))
                            .build()
            )
            .and(
                    route("order_get_route")
                            .GET("/api/orders", http())
                            .before(uri("lb://order-service"))
                            .build()
            );
}
```

**Verify:** Start all services (discovery-server, catalog-service, order-service, api-gateway), then test through the
gateway:

```bash
mvn -pl api-gateway spring-boot:run

# All traffic now goes through port 8080
curl http://localhost:8080/api/books
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'
curl http://localhost:8080/api/orders
```

> **Discussion point:** The `lb://` prefix tells the gateway to use client-side load balancing via Eureka. If you had 3
> instances of catalog-service, traffic would be distributed.

---

## Section 4: Centralized Configuration (25 min)

### Exercise 4A: Config Server Setup (10 min)

Start the config server:

```bash
mvn -pl config-server spring-boot:run
```

Verify it's running: http://localhost:8888/actuator/health

Now edit `config-server/src/main/resources/config-repo/catalog-service.properties`

Complete **TODO 10** — Add catalog configuration:

```properties
spring.datasource.url=jdbc:h2:mem:catalogdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

app.greeting=Hello from Config Server!
```

### Exercise 4B: Understand the Config Client (5 min)

Look at `catalog-service/src/main/resources/application.properties` — notice the `spring.config.import` property:

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

This was pre-configured so the app starts without Config Server (the `optional:` prefix). Now that Config Server
is running, **restart catalog-service** and it will pick up the configuration from Config Server.

Verify by checking: http://localhost:8888/catalog-service/default — you should see the config values you added.

### Exercise 4C: Dynamic Refresh (10 min)

Open `catalog-service/src/main/java/com/bookshop/catalog/book/GreetingController.java`

Complete **TODO 12**:

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@RestController
@RefreshScope
public class GreetingController {

    @Value("${app.greeting:Hello from local config!}")
    private String greeting;

    @GetMapping("/api/greeting")
    public String greeting() {
        return greeting;
    }
}
```

**Verify:**

```bash
# Restart catalog-service
curl http://localhost:8081/api/greeting
# Should return: "Hello from Config Server!"

# Change app.greeting in config-repo/catalog-service.properties to something else
# Then trigger refresh:
curl -X POST http://localhost:8081/actuator/refresh
curl http://localhost:8081/api/greeting
# Should return the new value!
```

---

## BREAK (10 min)

---

## Section 5: Resilience Patterns (25 min)

### Exercise 5A: Circuit Breaker (15 min)

Open `order-service/src/main/java/com/bookshop/order/client/BookClient.java`

Complete **TODO 13** — Add circuit breaker to `getBookByIsbn`:

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "catalogService", fallbackMethod = "getBookFallback")
public Optional<BookResponse> getBookByIsbn(String isbn) {
    // ... existing implementation
}
```

Complete **TODO 14** — Add fallback method:

```java
private Optional<BookResponse> getBookFallback(String isbn, Throwable t) {
    log.warn("Fallback: catalog-service unavailable for ISBN {}: {}", isbn, t.getMessage());
    return Optional.empty();
}
```

### Exercise 5B: Retry (10 min)

Complete **TODO 15** — Add retry:

```java
import io.github.resilience4j.retry.annotation.Retry;

@CircuitBreaker(name = "catalogService", fallbackMethod = "getBookFallback")
@Retry(name = "catalogService")
public Optional<BookResponse> getBookByIsbn(String isbn) {
    // ... existing implementation
}
```

**Verify:**

```bash
# With all services running, place an order — should work
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'

# Stop catalog-service (Ctrl+C)
# Try placing another order — should fail gracefully
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'

# Check circuit breaker state
curl http://localhost:8082/actuator/health
```

> **Discussion point:** The circuit breaker opens after 50% of 5 calls fail. After 10 seconds, it moves to half-open and
> allows 3 test calls. Tune these values based on your SLAs.

---

## Section 6: Observability (20 min)

### Exercise 6A: Health Checks & Actuator (8 min)

Edit `order-service/src/main/resources/application.properties`

Complete **TODO 16** — Expose actuator endpoints:

```properties
management.endpoints.web.exposure.include=health,info,metrics,circuitbreakers,refresh
management.endpoint.health.show-details=always
```

**Verify:**

```bash
curl http://localhost:8082/actuator/health | jq .
# Should show: db, diskSpace, eureka, circuitBreakers
```

### Exercise 6B: Structured Logging (7 min)

Complete **TODO 17** in `order-service/src/main/resources/application.properties`:

```properties
logging.structured.format.console=ecs
```

Complete **TODO 18** in `OrderService.java` — Add logging:

```java
public Order placeOrder(CreateOrderRequest request) {
    log.info("Placing order for {} items", request.items().size());
    // ... rest of the method
}
```

Restart order-service and place an order — observe the structured JSON log output in the console.

### Exercise 6C: Custom Metrics (5 min)

Complete **TODO 19** in `OrderService.java` — Add counter after save:

```java
Order order = new Order(orderItems);
Order saved = orderRepository.save(order);
meterRegistry.

counter("orders.placed","status","success").

increment();
return saved;
```

**Verify:**

```bash
# Place a few orders, then check the metric
curl http://localhost:8082/actuator/metrics/orders.placed
```

---

## Section 7: Docker Compose (10 min)

_Instructor demo — run the full system in containers._

```bash
# Build all services
mvn clean package -DskipTests

# Run with Docker Compose
docker compose up --build
```

All services start in order thanks to `depends_on` with health checks.

Test the full system:

```bash
curl http://localhost:8080/api/books
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'
```

> **Discussion point:** In production, you would NOT rely on `depends_on` ordering. Services should be resilient to
> startup order — that's what circuit breakers and retries from Section 5 are for.

---

## Section 8: When (Not) to Use Microservices (15 min)

_Instructor-led discussion & Q&A._

Discussion topics:

- **This bookshop should be a monolith.** When would you actually split?
- The "distributed monolith" anti-pattern
- Operational cost: 5 things to deploy instead of 1
- What we did NOT cover:
    - Distributed transactions and sagas
    - Event-driven communication (Kafka, RabbitMQ)
    - API versioning
    - Security (OAuth2/JWT)
    - Distributed tracing (Zipkin/Tempo)
    - Kubernetes deployment
- Recommended next steps

---

## Quick Reference: All TODOs

| TODO | File                                | Section | Description                          |
|------|-------------------------------------|---------|--------------------------------------|
| 1    | `BookController.java`               | 2       | GET all books                        |
| 2    | `BookController.java`               | 2       | GET book by ISBN                     |
| 3    | `BookClient.java`                   | 2       | RestClient call to catalog-service   |
| 4    | `OrderService.java`                 | 2       | placeOrder business logic            |
| 5    | `OrderController.java`              | 2       | POST order endpoint                  |
| 6    | `OrderController.java`              | 2       | GET all orders endpoint              |
| 7    | `application.properties` (both)     | 3       | Eureka client config                 |
| 8    | `BookClient.java`                   | 3       | Service discovery URL + LoadBalanced |
| 9    | `GatewayRouteConfig.java`           | 3       | Gateway routes                       |
| 10   | `catalog-service.properties` (config-repo) | 4  | Move config to Config Server         |
| 11   | `application.properties` (both)     | 4       | Config import (pre-configured)       |
| 12   | `GreetingController.java`           | 4       | @RefreshScope + @Value               |
| 13   | `BookClient.java`                   | 5       | @CircuitBreaker                      |
| 14   | `BookClient.java`                   | 5       | Fallback method                      |
| 15   | `BookClient.java`                   | 5       | @Retry                               |
| 16   | `application.properties` (order)    | 6       | Actuator endpoints                   |
| 17   | `application.properties` (order)    | 6       | Structured logging                   |
| 18   | `OrderService.java`                 | 6       | Log statement                        |
| 19   | `OrderService.java`                 | 6       | Custom metric counter                |

---

## Troubleshooting

**Eureka registration takes too long:**
The discovery server is configured with a 5-second cache update interval. If services don't appear immediately, wait
10-15 seconds and refresh.

**Config Server not picking up changes:**
Make sure `spring.profiles.active=native` is set. The native profile reads from the classpath/filesystem, no Git
required.

**Circuit breaker not opening:**
Check the configuration in `application.properties`. The sliding window is 5 calls with a 50% failure threshold — you need at
least 3 failures in 5 calls.

**Port already in use:**
Kill the process using the port: `lsof -i :8081 | grep LISTEN` then `kill <PID>`
