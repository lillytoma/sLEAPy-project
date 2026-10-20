package com.sleapy.project.services;

import java.math.BigDecimal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sleapy.project.models.dtos.PriceAlertRequestDTO;
import com.sleapy.project.models.entities.ClientEntity;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationDeliveryService {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String fromAddress;

    public NotificationDeliveryService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${notification.mail.from:${spring.mail.username:}}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendAccountCreatedEmail(ClientEntity client) {
        if (client == null || client.getEmail() == null) return;
        sendEmail(client.getEmail(), "Welcome to sLEAPy Stocks",
                "Hi " + client.getFirstName() + ",\n\nYour sLEAPy Stocks account was created successfully.\n\nYou can now sign in and configure your notification preferences in Settings.");
    }

    public void sendPriceAlert(ClientEntity client, PriceAlertRequestDTO request) {
        if (client == null || request == null) return;
        String direction = "BUY".equalsIgnoreCase(request.getDirection()) ? "gained" : "dropped";
        String subject = "sLEAPy price alert: " + request.getSymbol();
        String body = String.format("%s has %s %s%% and is now trading at $%s.%n%nReview your watchlist before placing a trade.",
                request.getSymbol(), direction, format(request.getChangePercent()), format(request.getPrice()));
        if (request.isEmailEnabled()) sendEmail(client.getEmail(), subject, body);
        if (request.isPhoneEnabled()) {
            log.warn("Phone notification requested for client {} but no SMS provider is configured.", client.getId());
        }
    }

    private void sendEmail(String recipient, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.warn("Email notification skipped for {} because SMTP is not configured.", recipient);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }

    private String format(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
