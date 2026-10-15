package com.sleapy.project.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.sleapy.project.models.dtos.OrderStatusUpdateDTO;
import com.sleapy.project.services.OrderStatusNotificationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.task.scheduling.enabled=false")
class OrderStatusWebSocketIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private OrderStatusNotificationService notificationService;

    private WebSocketStompClient stompClient;
    private ThreadPoolTaskScheduler taskScheduler;

    @AfterEach
    void closeClient() throws Exception {
        if (stompClient != null) {
            stompClient.stop();
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
            taskScheduler = null;
        }
    }

    @Test
    void deliversOrderStatusesInOrder() throws Exception {
        BlockingQueue<OrderStatusUpdateDTO> events = new LinkedBlockingQueue<>();
        StompSession session = connect();
        awaitSubscription(session, events);

        notificationService.publish(null, 7L, "AAPL", "BUY", "PENDING", "Order received");
        notificationService.publish(null, 7L, "AAPL", "BUY", "ACCEPTED", "Order accepted");
        notificationService.publish(42L, 7L, "AAPL", "BUY", "FILLED", "Order filled");

        assertEquals(Set.of("PENDING", "ACCEPTED", "FILLED"), Set.copyOf(takeStatuses(events, 3)));
        session.disconnect();
    }

    @Test
    void deliversRejectedOrderStatus() throws Exception {
        BlockingQueue<OrderStatusUpdateDTO> events = new LinkedBlockingQueue<>();
        StompSession session = connect();
        awaitSubscription(session, events);

        notificationService.publish(null, 7L, null, "BUY", "REJECTED", "Insufficient cash");

        OrderStatusUpdateDTO event = events.poll(5, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals("REJECTED", event.getStatus());
        assertEquals("Insufficient cash", event.getMessage());
        session.disconnect();
    }

    @Test
    void receivesEventsAfterReconnect() throws Exception {
        BlockingQueue<OrderStatusUpdateDTO> events = new LinkedBlockingQueue<>();
        StompSession firstSession = connect();
        awaitSubscription(firstSession, events);
        firstSession.disconnect();

        StompSession reconnectedSession = connect();
        awaitSubscription(reconnectedSession, events);
        notificationService.publish(43L, 7L, "MSFT", "SELL", "FILLED", "Order filled after reconnect");

        OrderStatusUpdateDTO event = events.poll(5, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals("FILLED", event.getStatus());
        assertEquals(43L, event.getOrderId());
        reconnectedSession.disconnect();
    }

    @Test
    void synchronizesTwoSessionsForSameClient() throws Exception {
        BlockingQueue<OrderStatusUpdateDTO> firstEvents = new LinkedBlockingQueue<>();
        BlockingQueue<OrderStatusUpdateDTO> secondEvents = new LinkedBlockingQueue<>();
        StompSession firstSession = connect();
        StompSession secondSession = connect();
        awaitSubscription(firstSession, firstEvents);
        awaitSubscription(secondSession, secondEvents);
        firstEvents.clear();
        secondEvents.clear();

        notificationService.publish(44L, 7L, "NVDA", "BUY", "FILLED", "Order filled");

        assertEquals("FILLED", firstEvents.poll(5, TimeUnit.SECONDS).getStatus());
        assertEquals("FILLED", secondEvents.poll(5, TimeUnit.SECONDS).getStatus());
        firstSession.disconnect();
        secondSession.disconnect();
    }

    private StompSession connect() throws Exception {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);
        CompletableFuture<StompSession> connection = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws/prices/websocket",
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        throw new AssertionError("STOMP session error", exception);
                    }
                }
        );
        return connection.get(5, TimeUnit.SECONDS);
    }

    private StompFrameHandler handler(BlockingQueue<OrderStatusUpdateDTO> events) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return OrderStatusUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                events.add((OrderStatusUpdateDTO) payload);
            }
        };
    }

    private void awaitSubscription(StompSession session, BlockingQueue<OrderStatusUpdateDTO> events) throws InterruptedException {
        session.subscribe("/topic/orders/7", handler(events));
        for (int attempt = 0; attempt < 50; attempt++) {
            notificationService.publish(null, 7L, "TEST", "BUY", "PENDING", "subscription probe");
            OrderStatusUpdateDTO probe = events.poll(100, TimeUnit.MILLISECONDS);
            if (probe != null && "subscription probe".equals(probe.getMessage())) return;
        }
        throw new AssertionError("STOMP subscription did not receive a probe event");
    }

    private List<String> takeStatuses(BlockingQueue<OrderStatusUpdateDTO> events, int count) {
        return IntStream.range(0, count).mapToObj(index -> {
            try {
                OrderStatusUpdateDTO event = events.poll(5, TimeUnit.SECONDS);
                assertNotNull(event);
                return event.getStatus();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for order event", exception);
            }
        }).toList();
    }
}
