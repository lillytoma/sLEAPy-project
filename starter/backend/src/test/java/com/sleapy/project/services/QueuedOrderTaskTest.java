package com.sleapy.project.services;

import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.scheduled.QueuedOrderTask;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;

class QueuedOrderTaskTest {
    @Test
    void doesNotProcessOutsideRegularSession() {
        var orders = mock(OrderMapper.class);
        var hours = mock(MarketHoursService.class);
        var trades = mock(TradeService.class);
        new QueuedOrderTask(orders, hours, trades).executeOrders();
        verifyNoInteractions(orders, trades);
    }

    @Test
    void failedOrderDoesNotStopOtherQueuedOrders() {
        var orders = mock(OrderMapper.class);
        var hours = mock(MarketHoursService.class);
        var trades = mock(TradeService.class);
        when(hours.isMarketOpen()).thenReturn(true);
        when(orders.findQueuedIds()).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("Quote provider unavailable")).when(trades).executeQueued(1L);
        new QueuedOrderTask(orders, hours, trades).executeOrders();
        verify(trades).executeQueued(1L);
        verify(trades).executeQueued(2L);
    }
}
