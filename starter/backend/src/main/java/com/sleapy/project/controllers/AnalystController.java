package com.sleapy.project.controllers;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystSummaryDTO;
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
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!jwtService.verifyAnalystAuthorizationHeader(authorizationHeader)) {
            return unauthorized();
        }
        return ResponseEntity.ok(analystService.getSummary(clientId));
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

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Analyst authorization is required"));
    }
}
