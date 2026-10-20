package com.sleapy.project.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.models.dtos.PriceAlertRequestDTO;
import com.sleapy.project.services.NotificationDeliveryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class NotificationController {
    private final ClientMapper clientMapper;
    private final NotificationDeliveryService deliveryService;

    @PostMapping("/price-alert")
    public ResponseEntity<?> sendPriceAlert(@RequestBody PriceAlertRequestDTO request) {
        if (request.getClientId() == null || request.getSymbol() == null || request.getSymbol().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A client and instrument are required."));
        }
        return clientMapper.findById(request.getClientId())
                .map(client -> {
                    deliveryService.sendPriceAlert(client, request);
                    return ResponseEntity.ok(Map.of("sent", true));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
