package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.BuyOrderRequestDTO;
import com.sleapy.project.models.dtos.SellOrderRequestDTO;
import com.sleapy.project.models.dtos.TradeResponseDTO;
import com.sleapy.project.services.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TradeController {

    private final TradeService tradeService;

    /**
     * Execute a BUY order for a client.
     * 
     * Request body example:
     * {
     *   "clientId": 1,
     *   "instrumentId": 1,
     *   "quantity": 10
     * }
     * 
     * Test with:
     * curl -X POST http://localhost:8081/api/trades/buy \
     *   -H "Content-Type: application/json" \
     *   -d '{"clientId":1,"instrumentId":1,"quantity":10}'
     * 
     * @param request BuyOrderRequestDTO containing clientId, instrumentId, quantity
     * @return TradeResponseDTO with execution details on success
     */
    @PostMapping("/buy")
    public ResponseEntity<?> buyStock(@RequestBody BuyOrderRequestDTO request) {
        try {
            // Validate input
            if (request.getClientId() == null || request.getClientId() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Valid clientId required"));
            }
            if (request.getInstrumentId() == null || request.getInstrumentId() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Valid instrumentId required"));
            }
            if (request.getQuantity() == null || request.getQuantity().signum() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Quantity must be positive"));
            }

            TradeResponseDTO response = tradeService.buyStock(request);
            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // InsufficientCashException, runtime exceptions, etc.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Execute a SELL order for a client.
     * 
     * Request body example:
     * {
     *   "clientId": 1,
     *   "instrumentId": 1,
     *   "quantity": 5
     * }
     * 
     * Test with:
     * curl -X POST http://localhost:8081/api/trades/sell \
     *   -H "Content-Type: application/json" \
     *   -d '{"clientId":1,"instrumentId":1,"quantity":5}'
     * 
     * @param request SellOrderRequestDTO containing clientId, instrumentId, quantity
     * @return TradeResponseDTO with execution details on success
     */
    @PostMapping("/sell")
    public ResponseEntity<?> sellStock(@RequestBody SellOrderRequestDTO request) {
        try {
            // Validate input
            if (request.getClientId() == null || request.getClientId() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Valid clientId required"));
            }
            if (request.getInstrumentId() == null || request.getInstrumentId() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Valid instrumentId required"));
            }
            if (request.getQuantity() == null || request.getQuantity().signum() <= 0) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Quantity must be positive"));
            }

            TradeResponseDTO response = tradeService.sellStock(request);
            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // InsufficientSharesException, runtime exceptions, etc.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
