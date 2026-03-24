package com.bookshop.order.client;

public record BookResponse(Long id, String isbn, String title, String author, double price) {
}
