package com.bookshop.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClient.class);

    private final RestClient restClient;

    public BookClient(RestClient catalogRestClient) {
        this.restClient = catalogRestClient;
    }

    @Retryable(maxRetries = 3, delay = 500)
    public BookResponse getBookByIsbn(String isbn) {
        return restClient.get()
                .uri("/api/books/{isbn}", isbn)
                .retrieve()
                .body(BookResponse.class);
    }
}
