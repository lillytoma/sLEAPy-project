package com.sleapy.project.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationAuditLogService {
    private static final String LOG_FILE = "authentication.log";
    private final AuditLogWriter writer;

    public void log(String result, String event, String username, String ipAddress) {
        writer.append(LOG_FILE, String.join(" | ",
                writer.timestamp(),
                event,
                value(result),
                value(username),
                value(ipAddress)));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value.replace("|", "/");
    }
}
