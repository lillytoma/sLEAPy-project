package com.sleapy.project.controllers;

import com.sleapy.project.mappers.AuditLogMapper;
import com.sleapy.project.models.dtos.ActiveUsersResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for audit log and compliance dashboard endpoints
 * Provides endpoints for monitoring client activity and system usage
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@AllArgsConstructor
@CrossOrigin("http://localhost:4200")
public class ActiveUsersController {
    
    private final AuditLogMapper auditLogMapper;
    
    /**
     * Get count of active users in the last 24 hours
     * Used by dashboard widget: "Active Users Last 24 Hours"
     * 
     * @return Response with count of active users
     */
    @GetMapping("/active-users-24h")
    public ResponseEntity<?> getActiveUsersLast24Hours() {
        try {
            int activeUserCount = auditLogMapper.countActiveUsersLast24Hours();
            
            ActiveUsersResponse response = ActiveUsersResponse.builder()
                .activeUsersCount(activeUserCount)
                .timeperiod("last_24_hours")
                .message("Active users who logged in or attempted to login in the last 24 hours")
                .timestamp(System.currentTimeMillis())
                .build();
            
            log.info("Active users count retrieved: {}", activeUserCount);
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error retrieving active users count", e);
            return new ResponseEntity<>(
                "Error retrieving active users: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Health check endpoint for audit system
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<?> auditSystemHealth() {
        try {
            // Simple query to verify database connectivity
            auditLogMapper.countActiveUsersLast24Hours();
            return new ResponseEntity<>(
                "{\"status\": \"healthy\", \"module\": \"audit-log\"}",
                HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("Audit system health check failed", e);
            return new ResponseEntity<>(
                "{\"status\": \"unhealthy\", \"module\": \"audit-log\", \"error\": \"" + e.getMessage() + "\"}",
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
}
