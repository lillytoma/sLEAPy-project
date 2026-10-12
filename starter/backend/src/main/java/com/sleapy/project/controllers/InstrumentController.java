package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.InstrumentWithPriceDTO;
import com.sleapy.project.services.InstrumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for instruments and real-time market prices.
 * Endpoints:
 * - GET /api/instruments - all instruments with current prices
 * - GET /api/instruments/{symbol} - single instrument by symbol
 * - GET /api/instruments/search?q=query - search by symbol or name
 */
@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class InstrumentController {
    private final InstrumentService instrumentService;

    /**
     * Get all instruments with their current market prices.
     * Usage: GET /api/instruments
     * 
     * @return List of InstrumentWithPriceDTO objects
     */
    @GetMapping("")
    public ResponseEntity<List<InstrumentWithPriceDTO>> getAllInstruments() {
        try {
            List<InstrumentWithPriceDTO> instruments = instrumentService.getAllInstruments();
            return ResponseEntity.ok(instruments);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Get a single instrument by symbol with its current price.
     * Usage: GET /api/instruments/AAPL
     * 
     * @param symbol the instrument symbol (e.g., AAPL)
     * @return InstrumentWithPriceDTO if found, 404 if not found
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getInstrumentBySymbol(@PathVariable String symbol) {
        try {
            Optional<InstrumentWithPriceDTO> instrument = instrumentService.getInstrumentBySymbol(symbol.toUpperCase());
            if (instrument.isPresent()) {
                return ResponseEntity.ok(instrument.get());
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Instrument not found: " + symbol));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving instrument: " + e.getMessage()));
        }
    }

    /**
     * Search instruments by symbol or name (case-insensitive, partial match).
     * Usage: GET /api/instruments/search?q=apple
     *        GET /api/instruments/search?q=AAPL
     * 
     * @param query search term (symbol or company name)
     * @return List of matching InstrumentWithPriceDTO objects
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchInstruments(@RequestParam(name = "q", required = false) String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Search query parameter 'q' is required"));
            }

            List<InstrumentWithPriceDTO> results = instrumentService.searchInstruments(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error searching instruments: " + e.getMessage()));
        }
    }
}
