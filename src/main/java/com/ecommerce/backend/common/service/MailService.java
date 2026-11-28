package com.ecommerce.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private static final URI BREVO_API_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final JavaMailSender mailSender;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:noreply@ecommerce.com}")
    private String fromAddress;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

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
        String[] recipients = message.getTo();
        String toEmail = (recipients != null && recipients.length > 0) ? recipients[0] : null;

        if (brevoApiKey != null && !brevoApiKey.isBlank() && toEmail != null) {
            sendViaBrevo(toEmail, message.getSubject(), message.getText(), context);
            return;
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("SMTP not configured; skipping {} email to {}. Content: {}",
                    context, Arrays.toString(recipients), message.getText());
            return;
        }
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send {} email to {}: {}. Content: {}",
                    context, Arrays.toString(recipients), ex.getMessage(), message.getText());
        }
    }

    private void sendViaBrevo(String toEmail, String subject, String textContent, String context) {
        try {
            String body = "{\"sender\":{\"email\":" + jsonEscape(fromAddress) + ",\"name\":\"Ecommerce Store\"},"
                    + "\"to\":[{\"email\":" + jsonEscape(toEmail) + "}],"
                    + "\"subject\":" + jsonEscape(subject) + ","
                    + "\"textContent\":" + jsonEscape(textContent) + "}";

            HttpRequest request = HttpRequest.newBuilder(BREVO_API_URI)
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Sent {} email via Brevo to {}", context, toEmail);
            } else {
                log.warn("Brevo returned {} for {} email to {}: {}. Content: {}",
                        response.statusCode(), context, toEmail, response.body(), textContent);
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to send {} email via Brevo to {}: {}. Content: {}",
                    context, toEmail, ex.getMessage(), textContent);
        }
    }

    private static String jsonEscape(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
