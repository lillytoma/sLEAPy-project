package com.sleapy.project.controllers;

import com.sleapy.project.services.PricingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "http://localhost:4200")
public class PriceController {
  
  private final PricingService pricingService;
  
  public PriceController(PricingService pricingService) {
    this.pricingService = pricingService;
  }
  
  /**
   * Get the current price for a specific stock symbol
   */
  @GetMapping("/{symbol}")
  public Map<String, Object> getPrice(@PathVariable String symbol) {
    Map<String, Object> response = new HashMap<>();
    BigDecimal price = pricingService.getPrice(symbol);
    response.put("symbol", symbol);
    response.put("price", price != null ? price.doubleValue() : 0.0);
    response.put("found", price != null);
    return response;
  }
  
  /**
   * Get prices for multiple symbols
   */
  @PostMapping("/batch")
  public Map<String, Double> getPrices(@RequestBody String[] symbols) {
    Map<String, Double> prices = new HashMap<>();
    for (String symbol : symbols) {
      BigDecimal price = pricingService.getPrice(symbol);
      prices.put(symbol, price != null ? price.doubleValue() : 0.0);
    }
    return prices;
  }
  
  /**
   * Get prices for all tracked symbols
   */
  @GetMapping("/all")
  public Map<String, Double> getAllPrices() {
    String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "TSLA"};
    Map<String, Double> prices = new HashMap<>();
    for (String symbol : symbols) {
      BigDecimal price = pricingService.getPrice(symbol);
      prices.put(symbol, price != null ? price.doubleValue() : 0.0);
    }
    return prices;
  }
}
