package com.example.pricing_calculation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ResponseTimeFilter extends OncePerRequestFilter {

    private final long responseSlaMs;

    public ResponseTimeFilter(@Value("${parking.sla.response-ms:3000}") long responseSlaMs) {
        this.responseSlaMs = responseSlaMs;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            response.setHeader("X-Response-Time-Ms", Long.toString(elapsedMs));
            response.setHeader("X-Response-SLA-Ms", Long.toString(responseSlaMs));
            response.setHeader("X-Response-SLA", elapsedMs <= responseSlaMs ? "PASS" : "WARN");
        }
    }
}
