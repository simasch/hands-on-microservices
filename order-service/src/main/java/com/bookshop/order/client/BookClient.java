package com.bookshop.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClient.class);

    private final RestClient restClient;

    public BookClient(RestClient.Builder builder) {
        this.restClient = builder
                // ============================================================
                // Section 2 - The base URL is hardcoded for now
                // Section 3 - Exercise: You will change this to use service discovery
                // ============================================================
                .baseUrl("http://localhost:8081")
                // ============================================================
                // TODO 8: Replace this constructor to accept a pre-built RestClient:
                //   Change constructor to: public BookClient(RestClient catalogRestClient)
                //   And set: this.restClient = catalogRestClient;
                //   Then create a @Configuration class RestClientConfig.java with a
                //   @Bean that builds a load-balanced RestClient using LoadBalancerClient.
                //   See the workshop guide for the full RestClientConfig code.
                // ============================================================
                .build();
    }

    // ============================================================
    // Section 2 - Exercise: Inter-Service Communication
    // ============================================================

    // TODO 3: Create a method to get a book by ISBN from catalog-service
    //   Method signature: public BookResponse getBookByIsbn(String isbn)
    //   Use restClient.get()
    //       .uri("/api/books/{isbn}", isbn)
    //       .retrieve()
    //       .body(BookResponse.class)
    //   Just return the result — don't catch exceptions here.
    //
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
