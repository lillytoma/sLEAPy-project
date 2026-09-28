package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.sleapy.project.config.APIRouting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.sleapy.project.models.dtos.HoldingDTO;
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
}
