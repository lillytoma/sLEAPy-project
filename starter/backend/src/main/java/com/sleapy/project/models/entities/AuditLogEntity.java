package com.sleapy.project.models.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry for client activities
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogEntity {
    private Long auditId;
    private Long clientId;
    private String activityType;      // ClientLoginAttempted, ClientLoginSucceeded, SignupCompleted
    private LocalDateTime timestamp;
    private String ipAddress;
    private String details;           // JSON string with additional context
    private String sourceSystem;      // e.g., "trading-app", "etl-pipeline"
}
