package com.sleapy.project.services;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sleapy.project.models.dtos.OrderStatusUpdateDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderStatusNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Long orderId, Long clientId, String symbol, String tradeType, String status, String message) {
        if (clientId == null) return;

        OrderStatusUpdateDTO update = new OrderStatusUpdateDTO(
                orderId,
                clientId,
                symbol,
                tradeType,
                status,
                message,
                LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/orders/" + clientId, update);
    }
}