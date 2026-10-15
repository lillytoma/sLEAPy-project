package com.sleapy.project.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sleapy.project.models.dtos.OrderStatusUpdateDTO;

@ExtendWith(MockitoExtension.class)
class OrderStatusNotificationServiceTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Captor
    private ArgumentCaptor<OrderStatusUpdateDTO> updateCaptor;

    @Test
    void publishesStatusUpdateToClientTopic() {
        OrderStatusNotificationService service = new OrderStatusNotificationService(messagingTemplate);

        service.publish(42L, 7L, "AAPL", "BUY", "FILLED", "Order filled");

        verify(messagingTemplate).convertAndSend(eq("/topic/orders/7"), updateCaptor.capture());
        OrderStatusUpdateDTO update = updateCaptor.getValue();
        assertEquals(42L, update.getOrderId());
        assertEquals(7L, update.getClientId());
        assertEquals("AAPL", update.getSymbol());
        assertEquals("BUY", update.getTradeType());
        assertEquals("FILLED", update.getStatus());
        assertEquals("Order filled", update.getMessage());
    }

    @Test
    void doesNotPublishWithoutClientId() {
        OrderStatusNotificationService service = new OrderStatusNotificationService(messagingTemplate);

        service.publish(42L, null, "AAPL", "BUY", "FILLED", "Order filled");

        verifyNoInteractions(messagingTemplate);
    }
}
