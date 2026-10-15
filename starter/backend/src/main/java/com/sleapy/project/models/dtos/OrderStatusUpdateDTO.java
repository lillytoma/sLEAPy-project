package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdateDTO {
    private Long orderId;
    private Long clientId;
    private String symbol;
    private String tradeType;
    private String status;
    private String message;
    private String timestamp;
}