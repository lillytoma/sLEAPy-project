package com.sleapy.project.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class PricingService {
  
  private final RedisTemplate<String, String> redisTemplate;
  private static final long PRICE_EXPIRY_HOURS = 24;
  
  public PricingService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  
  /**
   * Get the current price for a stock symbol from Redis cache
   */
  public BigDecimal getPrice(String symbol) {
    String key = "price:" + symbol.toUpperCase();
    String price = redisTemplate.opsForValue().get(key);
    
    if (price != null && !price.isEmpty()) {
      try {
        return new BigDecimal(price);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
  
  /**
   * Set/update the price for a stock symbol in Redis cache
   */
  public void setPrice(String symbol, BigDecimal price) {
    String key = "price:" + symbol.toUpperCase();
    redisTemplate.opsForValue().set(key, price.toPlainString(), PRICE_EXPIRY_HOURS, TimeUnit.HOURS);
  }
  
  /**
   * Check if a price exists in cache
   */
  public boolean priceExists(String symbol) {
    String key = "price:" + symbol.toUpperCase();
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
  
  /**
   * Clear all cached prices (for testing)
   */
  public void clearAllPrices() {
    String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "TSLA"};
    for (String symbol : symbols) {
      String key = "price:" + symbol.toUpperCase();
      redisTemplate.delete(key);
    }
  }
}
