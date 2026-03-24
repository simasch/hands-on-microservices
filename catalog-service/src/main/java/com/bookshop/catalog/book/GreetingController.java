package com.bookshop.catalog.book;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    // ============================================================
    // Section 4 - Exercise: Centralized Configuration
    // ============================================================

    // TODO 12: Enable dynamic configuration refresh
    //   1. Add @RefreshScope annotation to this class
    //      (import org.springframework.cloud.context.config.annotation.RefreshScope)
    //   2. Inject the greeting property:
    //      @Value("${app.greeting:Hello from local config!}")
    //      private String greeting;
    //   3. Uncomment the endpoint below

    // @GetMapping("/api/greeting")
    // public String greeting() {
    //     return greeting;
    // }
}
