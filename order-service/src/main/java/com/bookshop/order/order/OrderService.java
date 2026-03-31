package com.bookshop.order.order;

import com.bookshop.order.client.BookClient;
import com.bookshop.order.client.BookResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository, BookClient bookClient, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
        this.meterRegistry = meterRegistry;
    }

    // ============================================================
    // Section 2 - Exercise: Place an Order
    // ============================================================

    public Order placeOrder(CreateOrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (var item : request.items()) {
            BookResponse book = bookClient.getBookByIsbn(item.isbn());
            orderItems.add(new OrderItem(book.isbn(), book.title(), item.quantity(), book.price()));
        }
        Order order = new Order(orderItems);
        return orderRepository.save(order);
    }

    // ============================================================
    // Section 5 - Exercise: Handle failures gracefully
    // ============================================================
    //   TODO 14: Wrap the bookClient call in a try/catch so the order
    //   fails gracefully when catalog-service is unavailable (after retries):
    //
    //     try {
    //         BookResponse book = bookClient.getBookByIsbn(item.isbn());
    //         orderItems.add(new OrderItem(book.isbn(), book.title(), item.quantity(), book.price()));
    //     } catch (Exception e) {
    //         log.warn("Could not validate book {}: {}", item.isbn(), e.getMessage());
    //         throw new IllegalStateException("catalog-service unavailable, please try again later");
    //     }
    //
    // ============================================================
    // Section 6 - Exercise: Observability
    // ============================================================
    //   TODO 18: Add a log statement before processing:
    //     log.info("Placing order for {} items", request.items().size());
    //
    //   TODO 19: After saving, increment a counter:
    //     meterRegistry.counter("orders.placed", "status", "success").increment();

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
