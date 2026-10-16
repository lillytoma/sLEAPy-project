package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for active users dashboard widget response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveUsersResponse {
    private int activeUsersCount;
    private String timeperiod;      // e.g., "last_24_hours"
    private String message;
    private long timestamp;
}
