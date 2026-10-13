package com.sleapy.project.scheduled;

import com.sleapy.project.websocket.PriceWebSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for refreshing market prices and broadcasting updates via WebSocket.
 * Runs every 5 seconds to fetch latest prices and push to connected clients.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledPriceRefreshTask {
    private final PriceWebSocketController priceWebSocketController;

    /**
     * Refresh market prices every 5 seconds and broadcast to WebSocket clients.
     * 
     * fixedDelay = 5000 milliseconds (5 seconds)
     * This method fetches updated prices from the service and broadcasts them
     * to all connected WebSocket clients at /topic/prices.
     */
    @Scheduled(fixedDelay = 5000)
    public void refreshPricesAndBroadcast() {
        try {
            log.debug("Refreshing market prices...");
            priceWebSocketController.broadcastPriceUpdates();
            log.debug("Market prices broadcasted successfully");
        } catch (Exception e) {
            log.error("Error refreshing and broadcasting prices: {}", e.getMessage(), e);
        }
    }
}
