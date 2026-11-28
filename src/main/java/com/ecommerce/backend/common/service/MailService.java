package com.ecommerce.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:noreply@ecommerce.com}")
    private String fromAddress;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your email");
        message.setText("Click this link to verify your account: " + verifyUrl);

        sendSafely(message, "verification");
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset password");
        message.setText("Click this link to reset password your account: " + resetUrl);

        sendSafely(message, "password-reset");
    }

    public void sendOrderCancellationEmail(String toEmail, Long orderId, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Order #" + orderId + " has been cancelled");
        message.setText("Dear Customer,\n\n"
                + "We would like to inform you that your order #" + orderId + " has been cancelled by our store.\n"
                + "Reason: " + reason + "\n\n"
                + "If you have already paid for this order, your refund will be processed within 3-5 business days.\n\n"
                + "If you have any questions, please contact our support team.\n\n"
                + "Thank you,\n"
                + "Ecommerce Team");

        sendSafely(message, "order-cancellation");
    }

    private void sendSafely(SimpleMailMessage message, String context) {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("SMTP not configured; skipping {} email to {}. Content: {}",
                    context, Arrays.toString(message.getTo()), message.getText());
            return;
        }
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send {} email to {}: {}. Content: {}",
                    context, Arrays.toString(message.getTo()), ex.getMessage(), message.getText());
        }
    }
}
