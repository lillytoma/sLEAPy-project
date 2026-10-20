package com.sleapy.project.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuditLogWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogWriter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    private final Path logDirectory;

    public AuditLogWriter(@Value("${app.audit-log-directory:../logs}") String logDirectory) {
        this.logDirectory = Path.of(logDirectory);
    }

    public synchronized void append(String fileName, String line) {
        try {
            Files.createDirectories(logDirectory);
            Files.writeString(
                    logDirectory.resolve(fileName),
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            LOGGER.error("Unable to append audit event to {}", fileName, exception);
        }
    }

    public String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
