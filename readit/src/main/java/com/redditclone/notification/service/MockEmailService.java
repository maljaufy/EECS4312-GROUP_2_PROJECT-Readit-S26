package com.redditclone.notification.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;

@Service
@Slf4j
public class MockEmailService implements EmailService{

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        // Circuit Breaker configuration
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate opens circuit
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        circuitBreaker = cbRegistry.circuitBreaker("emailService");

        // Retry configuration
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(e -> e instanceof RuntimeException)
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        retry = retryRegistry.retry("emailService");
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        Supplier<Void> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, () -> {
                    log.info("Attempting to send email to: {}", to);
                    doSendEmail(to, subject, body);
                    return null;
                })
        );

        try {
            decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Failed to send email after retries and circuit breaker: {}", e.getMessage());
            throw new RuntimeException("Email service unavailable", e);
        }
    }

    private void doSendEmail(String to, String subject, String body) {
        // Simulate external service failure 40% of the time
        if (random.nextDouble() < 0.4) {
            log.warn("Mock email service failed for: {}", to);
            throw new RuntimeException("Simulated email service failure");
        }

        // Simulate processing time
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Email sent successfully to: {} | Subject: {}", to, subject);
    }

    @Override
    public boolean isCircuitBreakerOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }
}
