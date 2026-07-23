package com.redditclone.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSenderService {

    private final EmailService emailService;

    public void sendEmailNotification(String to, String subject, String body) {
        try {
            emailService.sendEmail(to, subject, body);
            log.info("Email notification sent to: {}", to);
        } catch (RuntimeException e) {
            log.error("Failed to send email notification: {}", e.getMessage());
            // Fallback: log the notification or store in database for later
        }
    }

    public boolean isEmailCircuitBreakerOpen() {
        return emailService.isCircuitBreakerOpen();
    }
}
