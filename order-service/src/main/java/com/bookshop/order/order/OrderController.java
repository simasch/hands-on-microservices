package com.bookshop.order.order;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ============================================================
    // Section 2 - Exercise: Order REST API
    // ============================================================

    // TODO 5: Create a POST endpoint to place an order
    //   Accepts a @RequestBody CreateOrderRequest
    //   Delegates to orderService.placeOrder(request)
    //   Returns the created Order

    // TODO 6: Create a GET endpoint to list all orders
    //   Returns List<Order> from orderService.getAllOrders()
}
