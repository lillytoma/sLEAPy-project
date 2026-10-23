package com.sleapy.project.controllers;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.models.enums.AnalystPeriod;
import com.sleapy.project.services.AnalystService;
import com.sleapy.project.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(APIRouting.ANALYST_ENDPOINT)
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnalystController {
    private final AnalystService analystService;
    private final JwtService jwtService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) List<Long> clientIds,
            @RequestParam(defaultValue = "DAY") String period,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!jwtService.verifyAnalystAuthorizationHeader(authorizationHeader)) {
            return unauthorized();
        }
        AnalystPeriod selectedPeriod;
        try {
            selectedPeriod = AnalystPeriod.valueOf(period.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Period must be DAY, WEEK, MONTH, or YEAR"));
        }
        if (clientIds == null) {
            if (clientId != null && clientId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Customer IDs must be positive"));
            }
            return ResponseEntity.ok(analystService.getSummary(clientId, selectedPeriod));
        }
        if (clientIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Customer IDs must be positive"));
        }
        var selected = new java.util.LinkedHashSet<>(clientIds);
        if (clientId != null) {
            if (clientId <= 0) return ResponseEntity.badRequest().body(Map.of("error", "Customer IDs must be positive"));
            selected.add(clientId);
        }
        return ResponseEntity.ok(analystService.getSummaryForClients(List.copyOf(selected), selectedPeriod));
    }

    @GetMapping("/clients")
    public ResponseEntity<?> searchClients(
            @RequestParam String search,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!jwtService.verifyAnalystAuthorizationHeader(authorizationHeader)) {
            return unauthorized();
        }

        if (search.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search term is required"));
        }
        List<AnalystClientDTO> clients = analystService.searchClients(search);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/instrument-owners")
    public ResponseEntity<?> getInstrumentOwners(
            @RequestParam String symbol,
            @RequestParam(required = false) List<Long> clientIds,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!jwtService.verifyAnalystAuthorizationHeader(authorizationHeader)) return unauthorized();
        if (symbol.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Symbol is required"));
        if (clientIds != null && clientIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Customer IDs must be positive"));
        }
        return ResponseEntity.ok(analystService.getInstrumentOwners(symbol.trim(),
                clientIds == null ? List.of() : clientIds.stream().distinct().toList()));
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Analyst authorization is required"));
    }
}
