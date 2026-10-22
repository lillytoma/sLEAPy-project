package com.sleapy.project.controllers;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.services.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(APIRouting.ADMIN_ENDPOINT)
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {
    private static final long ACTIVE_STATUS_ID = 1L;
    private static final long SUSPENDED_STATUS_ID = 2L;

    private final ClientMapper clientMapper;
    private final HoldingMapper holdingMapper;
    private final OrderMapper orderMapper;
    private final JwtService jwtService;

    public AdminController(ClientMapper clientMapper, HoldingMapper holdingMapper,
                           OrderMapper orderMapper, JwtService jwtService) {
        this.clientMapper = clientMapper;
        this.holdingMapper = holdingMapper;
        this.orderMapper = orderMapper;
        this.jwtService = jwtService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        ResponseEntity<?> unauthorized = requireAdmin(authorization);
        if (unauthorized != null) return unauthorized;
        return ResponseEntity.ok(clientMapper.findAll().stream().map(this::userSummary).toList());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        ResponseEntity<?> unauthorized = requireAdmin(authorization);
        if (unauthorized != null) return unauthorized;
        return clientMapper.findById(id)
                .<ResponseEntity<?>>map(client -> ResponseEntity.ok(userDetail(client)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return updateStatus(id, SUSPENDED_STATUS_ID, authorization);
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return updateStatus(id, ACTIVE_STATUS_ID, authorization);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        ResponseEntity<?> unauthorized = requireAdmin(authorization);
        if (unauthorized != null) return unauthorized;
        if (clientMapper.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        clientMapper.deleteHoldingsByClientId(id);
        clientMapper.deleteOrdersByClientId(id);
        clientMapper.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> updateStatus(Long id, long statusId, String authorization) {
        ResponseEntity<?> unauthorized = requireAdmin(authorization);
        if (unauthorized != null) return unauthorized;
        if (clientMapper.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        clientMapper.updateStatus(id, statusId);
        return ResponseEntity.ok(Map.of("status", statusId == SUSPENDED_STATUS_ID ? "Suspended" : "Active"));
    }

    private ResponseEntity<?> requireAdmin(String authorization) {
        if (!jwtService.isAdminAuthorizationHeader(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Administrator access is required"));
        }
        return null;
    }

    private Map<String, Object> userSummary(ClientEntity client) {
        return Map.of("id", client.getId(), "username", client.getUsername(),
                "fullName", client.getFirstName() + " " + client.getLastName(),
                "email", client.getEmail(), "status", statusName(client),
                "cashBalance", client.getCashBalance(), "admin", client.isAdmin());
    }

    private Map<String, Object> userDetail(ClientEntity client) {
        List<Map<String, Object>> holdings = holdingMapper.findByClientId(client.getId()).stream()
                .map(this::holdingSummary).toList();
        List<Map<String, Object>> activity = orderMapper.findTransactionsByClientId(client.getId()).stream()
                .map(this::activitySummary).toList();
        String ssn = client.getSsn();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("summary", userSummary(client));
        detail.put("phoneNumber", client.getPhoneNumber());
        detail.put("ssnLast4", ssn == null || ssn.length() < 4 ? null : ssn.substring(ssn.length() - 4));
        detail.put("dateOfBirth", client.getDob());
        detail.put("streetAddress", client.getStreetAddress());
        detail.put("city", client.getCity());
        detail.put("state", client.getStateName());
        detail.put("zipCode", client.getZipCode());
        detail.put("region", client.getRegion());
        detail.put("holdings", holdings);
        detail.put("activity", activity);
        return detail;
    }

    private Map<String, Object> holdingSummary(HoldingEntity holding) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", holding.getId());
        result.put("symbol", holding.getInstrument().getSymbol());
        result.put("name", holding.getInstrument().getSymbolName());
        result.put("shares", holding.getQuantityShares());
        result.put("purchasePrice", holding.getPurchasePrice());
        result.put("currentPrice", holding.getInstrument().getCurrentPrice());
        return result;
    }

    private Map<String, Object> activitySummary(OrderEntity order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("type", order.getTradeType());
        result.put("symbol", order.getInstrument().getSymbol());
        result.put("quantity", order.getQuantity());
        result.put("price", order.getPurchasePrice());
        result.put("total", order.getQuantity().multiply(order.getPurchasePrice()));
        result.put("status", order.getStatus().getName());
        result.put("date", order.getTimeOfPurchase());
        return result;
    }

    private String statusName(ClientEntity client) {
        return client.getClientStatus() == null ? "Active" : client.getClientStatus().getName();
    }
}
