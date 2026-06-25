package com.example.pricing_calculation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Xin chào! Dự án Spring Boot Backend của bạn đã chạy thành công.";
    }
}
