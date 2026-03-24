package com.bookshop.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class GatewayRouteConfig {

    // ============================================================
    // Section 3 - Exercise: API Gateway Routes
    // ============================================================

    // TODO 9: Define routes for the API Gateway
    //   Create a @Bean method that returns RouterFunction<ServerResponse>
    //
    //   Route 1 - Catalog Service:
    //     route("catalog_route")
    //         .GET("/api/books/**", http())
    //         .filter(lb("catalog-service"))
    //         .build()
    //
    //   Route 2 - Order Service (POST):
    //     route("order_post_route")
    //         .POST("/api/orders", http())
    //         .filter(lb("order-service"))
    //         .build()
    //
    //   Route 3 - Order Service (GET):
    //     route("order_get_route")
    //         .GET("/api/orders", http())
    //         .filter(lb("order-service"))
    //         .build()
    //
    //   Combine them using .and():
    //     return route("catalog_route")
    //         .GET("/api/books/**", http())
    //         .filter(lb("catalog-service"))
    //     .build()
    //     .and(
    //         route("order_post_route")
    //             .POST("/api/orders", http())
    //             .filter(lb("order-service"))
    //         .build()
    //     )
    //     .and(
    //         route("order_get_route")
    //             .GET("/api/orders", http())
    //             .filter(lb("order-service"))
    //         .build()
    //     );
    //
    //   Note: lb() uses client-side load balancing via Eureka to resolve service names
    // ============================================================
}
