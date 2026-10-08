package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.sleapy.project.config.APIRouting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.dtos.CreateHoldingRequestDTO;
import com.sleapy.project.models.dtos.UpdateHoldingRequestDTO;
import com.sleapy.project.services.HoldingsService;



@RestController
@RequestMapping(APIRouting.HOLDINGS_ENDPOINT)
public class HoldingsController {
    private final HoldingsService holdingsService;

    HoldingsController(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<List<HoldingDTO>> getClientHoldings(@PathVariable Long clientId) {
        List<HoldingDTO> holdings = holdingsService.getClientHoldings(clientId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get the net worth (total portfolio value) for a specific client.
     * Includes cash balance + market value of all holdings.
     * GET /api/holdings/{clientId}/networth
     * 
     * @param clientId the client ID
     * @return ResponseEntity with net worth breakdown or error message
     */
    @GetMapping("/{clientId}/networth")
    public ResponseEntity<?> getNetWorth(@PathVariable Long clientId) {
        try {
            Map<String, Object> netWorth = holdingsService.getClientNetWorth(clientId);
            return ResponseEntity.ok(netWorth);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new holding for a client.
     * POST /api/holdings
     * 
     * @param request the create holding request
     * @return ResponseEntity with created HoldingDTO or error message
     */
    @PostMapping("")
    public ResponseEntity<?> createHolding(@RequestBody CreateHoldingRequestDTO request) {
        try {
            HoldingDTO holding = holdingsService.createHolding(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(holding);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an existing holding.
     * PUT /api/holdings/{holdingId}
     * 
     * @param holdingId the holding ID
     * @param request the update holding request
     * @return ResponseEntity with updated HoldingDTO or error message
     */
    @PutMapping("/{holdingId}")
    public ResponseEntity<?> updateHolding(@PathVariable Long holdingId, @RequestBody UpdateHoldingRequestDTO request) {
        try {
            HoldingDTO holding = holdingsService.updateHolding(holdingId, request);
            return ResponseEntity.ok(holding);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a holding by ID.
     * DELETE /api/holdings/{holdingId}
     * 
     * @param holdingId the holding ID to delete
     * @return ResponseEntity with success message or error
     */
    @DeleteMapping("/{holdingId}")
    public ResponseEntity<?> deleteHolding(@PathVariable Long holdingId) {
        try {
            holdingsService.deleteHolding(holdingId);
            return ResponseEntity.ok(Map.of("message", "Holding deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
