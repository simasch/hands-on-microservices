# Hands-on Microservices with Spring Boot — Workshop Guide

**Author:** [Simon Martinelli](https://martinelli.ch)

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

## Section 0: Setup Verification (10 min)

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

**Documentation:**
- [Spring Boot Reference — Web](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Spring Data JPA — Reference](https://docs.spring.io/spring-data/jpa/reference/)
- [RestClient — Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)

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

**Documentation:**
- [Spring Cloud Netflix Eureka — Service Discovery](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html)
- [Spring Cloud Gateway Server MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html)
- [Spring Cloud Gateway — Java Routes API](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/java-routes-api.html)
- [Spring Cloud LoadBalancer](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)

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

## Section 4: Centralized Configuration (15 min)

**Documentation:**
- [Spring Cloud Config — Server](https://docs.spring.io/spring-cloud-config/reference/server.html)
- [Spring Cloud Config — Client](https://docs.spring.io/spring-cloud-config/reference/client.html)
- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)

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

## Section 5: Resilience Patterns (25 min)

**Documentation:**
- [Spring Framework — Resilience](https://docs.spring.io/spring-framework/reference/core/resilience.html)
- [Spring Framework — @Retryable](https://docs.spring.io/spring-framework/reference/core/resilience.html#retryable)

Spring Framework 7 includes **built-in retry support** via `@Retryable` — no external libraries needed.
The `@EnableResilientMethods` configuration is already provided in `ResilientConfig.java`.

### Exercise 5A: Add Retry to BookClient (15 min)

Open `order-service/src/main/java/com/bookshop/order/client/BookClient.java`

Notice that `ResilientConfig.java` already enables `@EnableResilientMethods` — this activates Spring Framework's
built-in retry support. No external libraries needed.

Complete **TODO 13** — Add `@Retryable` to `getBookByIsbn`:

```java
import org.springframework.resilience.annotation.Retryable;

@Retryable(maxRetries = 3, delay = 500)
public BookResponse getBookByIsbn(String isbn) {
    return restClient.get()
            .uri("/api/books/{isbn}", isbn)
            .retrieve()
            .body(BookResponse.class);
}
```

The method stays clean — no try/catch. `@Retryable` handles retries transparently. If all retries
fail, the exception propagates to the caller.

### Exercise 5B: Handle Failure in OrderService (10 min)

Open `order-service/src/main/java/com/bookshop/order/order/OrderService.java`

Complete **TODO 14** — Wrap the `bookClient` call so the order fails gracefully when catalog-service
is unavailable (after retries are exhausted):

```java
try {
    BookResponse book = bookClient.getBookByIsbn(item.isbn());
    orderItems.add(new OrderItem(book.isbn(), book.title(), item.quantity(), book.price()));
} catch (Exception e) {
    log.warn("Could not validate book {}: {}", item.isbn(), e.getMessage());
    throw new IllegalStateException("catalog-service unavailable, please try again later");
}
```

This is a cleaner separation of concerns:
- **BookClient** is a pure HTTP client — `@Retryable` handles transient failures
- **OrderService** decides what to do when the service is truly unavailable

**Verify:**

```bash
# With all services running, place an order — should work
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'

# Stop catalog-service (Ctrl+C)
# Try placing another order — should fail with a clear error after retries
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items": [{"isbn": "978-0-13-468599-1", "quantity": 1}]}'

# Check the logs — you should see retry attempts and the warning message
```

> **Discussion point:** `@Retryable` is built into Spring Framework 7 — no external dependencies needed.
> The retry is transparent to the caller. For more advanced patterns (circuit breaker state machine,
> bulkheads, rate limiters), you can add Resilience4j on top.

---

## Section 6: Observability (15 min)

**Documentation:**
- [Spring Boot Actuator — Production-ready Features](https://docs.spring.io/spring-boot/reference/actuator/index.html)
- [Spring Boot — Structured Logging](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured)
- [Micrometer — Documentation](https://docs.micrometer.io/micrometer/reference/)

### Exercise 6A: Health Checks & Actuator (8 min)

Edit `order-service/src/main/resources/application.properties`

Complete **TODO 16** — Expose actuator endpoints:

```properties
management.endpoints.web.exposure.include=health,info,metrics,refresh
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

**Documentation:**
- [Spring Boot — Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/docker-compose.html)
- [Docker Compose — Reference](https://docs.docker.com/compose/compose-file/)

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
| 13   | `BookClient.java`                   | 5       | @Retryable                           |
| 14   | `BookClient.java`                   | 5       | Fallback method                      |
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

---

## Further Reading

### Spring Boot & Spring Cloud
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/)
- [Spring Cloud Reference Documentation](https://docs.spring.io/spring-cloud/reference/)
- [Spring Cloud Netflix (Eureka)](https://docs.spring.io/spring-cloud-netflix/reference/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/reference/)
- [Spring Cloud Circuit Breaker](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)

### Resilience & Observability
- [Spring Framework — Resilience](https://docs.spring.io/spring-framework/reference/core/resilience.html)
- [Micrometer Documentation](https://docs.micrometer.io/micrometer/reference/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/)

### Architecture & Patterns
- [microservices.io — Patterns](https://microservices.io/patterns/index.html)
- [Martin Fowler — Microservices](https://martinfowler.com/articles/microservices.html)
- [Sam Newman — Building Microservices (book)](https://samnewman.io/books/building_microservices_2nd_edition/)
- [Chris Richardson — Microservices Patterns (book)](https://microservices.io/book)
