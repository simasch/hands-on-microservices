package com.bookshop.order.order;

import java.util.List;

public record CreateOrderRequest(List<OrderItemRequest> items) {

    public record OrderItemRequest(String isbn, int quantity) {}
}
