package com.redditclone.notification.ui;

import com.redditclone.notification.service.MockEmailService;
import com.redditclone.notification.service.NotificationSenderService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

@Route("admin/circuit-breaker")
@PageTitle("Circuit Breaker Dashboard | Reddit Clone")
@UIScope
public class CircuitBreakerDashboardView extends VerticalLayout {

        @Autowired
        private NotificationSenderService notificationSenderService;

        @Autowired
        private MockEmailService mockEmailService;

        private final TextField emailField = new TextField("Email Address");
        private final TextField subjectField = new TextField("Subject");
        private final TextField bodyField = new TextField("Message");
        private final Button sendButton = new Button("Send Test Email");
        private final Paragraph statusDisplay = new Paragraph("Status: CLOSED");

        public CircuitBreakerDashboardView() {
            setSizeFull();
            setAlignItems(Alignment.CENTER);

            add(
                    new H2("Circuit Breaker Dashboard"),
                    new Paragraph("Test the email circuit breaker with mock emails."),
                    emailField,
                    subjectField,
                    bodyField,
                    sendButton,
                    statusDisplay
            );

            // Update status periodically
            updateStatus();

            sendButton.addClickListener(e -> sendTestEmail());
        }

        private void sendTestEmail() {
            String email = emailField.getValue();
            if (email == null || email.isEmpty()) {
                Notification.show("Please enter an email address", 3000, Notification.Position.MIDDLE);
                return;
            }

            String subject = subjectField.getValue();
            String body = bodyField.getValue();

            try {
                notificationSenderService.sendEmailNotification(email, subject, body);
                Notification.show("Email sent successfully!", 3000, Notification.Position.MIDDLE);
            } catch (Exception e) {
                Notification.show("Failed to send email: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }

            updateStatus();
        }

        private void updateStatus() {
            boolean isOpen = notificationSenderService.isEmailCircuitBreakerOpen();
            statusDisplay.setText("Status: " + (isOpen ? "OPEN" : "CLOSED"));
            statusDisplay.getStyle().set("color", isOpen ? "red" : "green");
            statusDisplay.getStyle().set("font-weight", "bold");
        }
}
