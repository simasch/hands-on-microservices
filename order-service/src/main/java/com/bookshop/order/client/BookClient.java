package com.bookshop.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClient.class);

    private final RestClient restClient;

    public BookClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public BookResponse getBookByIsbn(String isbn) {
        return restClient.get()
                .uri("/api/books/{isbn}", isbn)
                .retrieve()
                .body(BookResponse.class);
    }

    // ============================================================
    // Section 5 - Exercise: Resilience Patterns
    // ============================================================
    // TODO 13: Add @Retryable annotation to the method above
    //   @Retryable(maxRetries = 3, delay = 500)
    //   (import org.springframework.resilience.annotation.Retryable)
    //
    //   This uses Spring Framework's built-in retry support.
    //   When catalog-service is down, the call will be retried
    //   up to 3 times with a 500ms delay between attempts.
    //   If all retries fail, the exception propagates to the caller.
}
