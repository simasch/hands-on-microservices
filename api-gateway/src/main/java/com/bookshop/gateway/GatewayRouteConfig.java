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

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("catalog_route")
                .GET("/api/books/**", http())
                .filter(lb("catalog-service"))
                .build()
                .and(
                        route("order_post_route")
                                .POST("/api/orders", http())
                                .filter(lb("order-service"))
                                .build()
                )
                .and(
                        route("order_get_route")
                                .GET("/api/orders", http())
                                .filter(lb("order-service"))
                                .build()
                );
    }
}
