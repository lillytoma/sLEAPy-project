package com.sleapy.project.scheduled;

import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.services.MarketHoursService;
import com.sleapy.project.services.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueuedOrderTask {
    private final OrderMapper orderMapper;
    private final MarketHoursService marketHoursService;
    private final TradeService tradeService;

    @Scheduled(fixedDelay = 30000)
    public void executeOrders() {
        if (!marketHoursService.isMarketOpen()) return;
        for (Long id : orderMapper.findQueuedIds()) {
            try {
                tradeService.executeQueued(id);
            } catch (RuntimeException ex) {
                log.error("Queued order {} could not execute; retained for retry", id, ex);
            }
        }
    }
}
