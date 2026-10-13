package com.sleapy.project.websocket;

import com.sleapy.project.models.dtos.PriceUpdateDTO;
import com.sleapy.project.services.InstrumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

/**
 * WebSocket controller for broadcasting real-time market price updates.
 * Prices are pushed to all connected clients at /topic/prices.
 */
@Controller
@RequiredArgsConstructor
public class PriceWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final InstrumentService instrumentService;

    /**
     * Broadcast price updates to all connected WebSocket clients.
     * This method is called by the scheduled price refresh task.
     * 
     * Sends all price updates to /topic/prices destination.
     * Connected clients receive updates in real-time.
     */
    public void broadcastPriceUpdates() {
        Map<String, PriceUpdateDTO> priceUpdates = instrumentService.refreshPrices();
        
        if (!priceUpdates.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/prices", priceUpdates);
        }
    }

    /**
     * Broadcast a single price update to all connected clients.
     * @param priceUpdate the price update to broadcast
     */
    public void broadcastSinglePrice(PriceUpdateDTO priceUpdate) {
        if (priceUpdate != null) {
            messagingTemplate.convertAndSend("/topic/prices/" + priceUpdate.getSymbol(), priceUpdate);
        }
    }
}
