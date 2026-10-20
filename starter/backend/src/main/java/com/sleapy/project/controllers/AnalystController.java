package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.services.AnalystService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.sleapy.project.models.dtos.AnalystClientDTO;
import java.util.List;

@RestController
@RequestMapping("/api/analyst")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnalystController {
    private final AnalystService analystService;

    @GetMapping("/summary")
    public ResponseEntity<AnalystSummaryDTO> getSummary(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(analystService.getSummary(clientId));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<AnalystClientDTO>> findClients(
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(analystService.findClients(search));
    }
}
