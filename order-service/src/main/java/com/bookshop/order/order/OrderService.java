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

    // TODO 4: Implement placeOrder(CreateOrderRequest request)
    //   - For each item in request.items():
    //     1. Call bookClient.getBookByIsbn(item.isbn())
    //     2. If the book is not found, throw an IllegalArgumentException("Book not found: " + isbn)
    //     3. Create an OrderItem with isbn, title, quantity, and price from the book
    //   - Create and save the Order with all OrderItems
    //   - Return the saved Order
    //
    //   Hint:
    //     List<OrderItem> orderItems = new ArrayList<>();
    //     for (var item : request.items()) {
    //         BookResponse book = bookClient.getBookByIsbn(item.isbn())
    //             .orElseThrow(() -> new IllegalArgumentException("Book not found: " + item.isbn()));
    //         orderItems.add(new OrderItem(book.isbn(), book.title(), item.quantity(), book.price()));
    //     }
    //     Order order = new Order(orderItems);
    //     return orderRepository.save(order);
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
