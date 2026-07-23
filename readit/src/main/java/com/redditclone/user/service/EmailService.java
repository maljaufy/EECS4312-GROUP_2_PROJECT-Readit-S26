package com.redditclone.user.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    boolean isCircuitBreakerOpen();
}
