package com.sleapy.project.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderAuditLogService {
    private static final String LOG_FILE = "orders.log";
    private final AuditLogWriter writer;

    public void log(Long orderId, String username, String side, String symbol, BigDecimal cashAmount,
                    BigDecimal sharesQuantity, String status) {
        writer.append(LOG_FILE, String.join(" | ",
                writer.timestamp(),
                orderId == null ? "-" : orderId.toString(),
                value(username),
                value(side),
                value(symbol),
                cashAmount == null ? "-" : cashAmount.toPlainString(),
                sharesQuantity == null ? "-" : sharesQuantity.toPlainString(),
                value(status)));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value.replace("|", "/");
    }
}
