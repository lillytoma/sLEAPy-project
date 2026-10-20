package com.sleapy.project.controllers;

import com.sleapy.project.exceptions.InsufficientCashException;
import com.sleapy.project.exceptions.InsufficientSharesException;
import com.sleapy.project.exceptions.OrderValidationException;
import com.sleapy.project.models.dtos.BuyOrderRequestDTO;
import com.sleapy.project.models.dtos.SellOrderRequestDTO;
import com.sleapy.project.models.dtos.TradeResponseDTO;
import com.sleapy.project.services.TradeService;
import com.sleapy.project.services.OrderStatusNotificationService;
import com.sleapy.project.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final OrderStatusNotificationService orderStatusNotificationService;
    private final JwtService jwtService;

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
    public ResponseEntity<?> buyStock(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody BuyOrderRequestDTO request) {
        ResponseEntity<?> unauthorized = verifyAuthorization(authorizationHeader);
        if (unauthorized != null) {
            return unauthorized;
        }
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

            TradeResponseDTO response = tradeService.submitBuyOrder(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (NoSuchElementException e) {
            notifyRejected(request.getClientId(), "BUY", "Order could not be completed");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "The requested trading information was not found."));
        } catch (IllegalArgumentException e) {
            notifyRejected(request.getClientId(), "BUY", "Order details are invalid");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Please check the order details and try again."));
        } catch (InsufficientCashException e) {
            notifyRejected(request.getClientId(), "BUY", "Insufficient cash");
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "There is not enough available cash to place this order."));
        } catch (OrderValidationException e) {
            notifyRejected(request.getClientId(), "BUY", "Order validation failed");
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("success", false, "status", "REJECTED", "error", "ORDER_REJECTED",
                    "message", "The order could not be completed. Please check your account and order details.",
                    "rule", "ORDER_VALIDATION_FAILED"));
        } catch (Exception e) {
            notifyRejected(request.getClientId(), "BUY", "Order could not be completed");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "We could not complete the order. Please try again."));
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
    public ResponseEntity<?> sellStock(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody SellOrderRequestDTO request) {
        ResponseEntity<?> unauthorized = verifyAuthorization(authorizationHeader);
        if (unauthorized != null) {
            return unauthorized;
        }
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

            TradeResponseDTO response = tradeService.submitSellOrder(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (NoSuchElementException e) {
            notifyRejected(request.getClientId(), "SELL", "Order could not be completed");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "The requested trading information was not found."));
        } catch (IllegalArgumentException e) {
            notifyRejected(request.getClientId(), "SELL", "Order details are invalid");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Please check the order details and try again."));
        } catch (InsufficientSharesException e) {
            notifyRejected(request.getClientId(), "SELL", "Insufficient shares");
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "There are not enough available shares to place this order."));
        } catch (OrderValidationException e) {
            notifyRejected(request.getClientId(), "SELL", "Order validation failed");
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("success", false, "status", "REJECTED", "error", "ORDER_REJECTED",
                    "message", "The order could not be completed. Please check your account and order details.",
                    "rule", "ORDER_VALIDATION_FAILED"));
        } catch (Exception e) {
            notifyRejected(request.getClientId(), "SELL", "Order could not be completed");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "We could not complete the order. Please try again."));
        }
    }

    private void notifyRejected(Long clientId, String tradeType, String message) {
        orderStatusNotificationService.publish(null, clientId, null, tradeType, "REJECTED", message);
    }

    private ResponseEntity<?> verifyAuthorization(String authorizationHeader) {
        if (jwtService.isAdminAuthorizationHeader(authorizationHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Administrators cannot place trades"));
        }
        if (jwtService.verifyAuthorizationHeader(authorizationHeader)) {
            return null;
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid or missing JWT token"));
    }
}
