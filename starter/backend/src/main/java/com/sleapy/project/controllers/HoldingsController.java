package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.sleapy.project.config.APIRouting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.sleapy.project.models.HoldingDTO;



@RestController
@RequestMapping(APIRouting.HOLDINGS_ENDPOINT)
public class HoldingsController {
    @GetMapping("/{clientId}")
    public ResponseEntity<List<HoldingDTO>> getClientHoldings(@PathVariable Long clientId) {
        return ResponseEntity.ok(List.of());
    }
}
