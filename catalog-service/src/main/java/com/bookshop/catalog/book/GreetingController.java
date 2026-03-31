package com.bookshop.catalog.book;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class GreetingController {

    @Value("${app.greeting:Hello from local config!}")
    private String greeting;

    @GetMapping("/api/greeting")
    public String greeting() {
        return greeting;
    }
}
