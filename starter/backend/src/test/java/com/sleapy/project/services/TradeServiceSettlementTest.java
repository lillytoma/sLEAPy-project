package com.sleapy.project.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.MarketPriceMapper;
import com.sleapy.project.mappers.OrderMapper;
import com.sleapy.project.mappers.OrderStatusMapper;
import com.sleapy.project.models.dtos.BuyOrderRequestDTO;
import com.sleapy.project.models.dtos.SellOrderRequestDTO;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.ClientStatus;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.models.entities.OrderEntity;
import com.sleapy.project.models.entities.OrderStatus;

@ExtendWith(MockitoExtension.class)
class TradeServiceSettlementTest {
    @Mock
    private ClientMapper clientMapper;

    @Mock
    private InstrumentMapper instrumentMapper;

    @Mock
    private MarketPriceMapper marketPriceMapper;

    @Mock
    private HoldingMapper holdingMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderStatusMapper orderStatusMapper;

    @Mock
    private OrderStatusNotificationService notificationService;

    @Mock
    private PriceService priceService;

    @Mock
    private MarketHoursService marketHoursService;

    private TradeService tradeService;

    private ClientEntity client;
    private InstrumentEntity instrument;
    private OrderStatus filledStatus;

    private OrderStatus status(Long id, String name) {
        OrderStatus status = new OrderStatus();
        status.setId(id);
        status.setName(name);
        return status;
    }

    private OrderEntity queueOrder(boolean buy) {
        when(marketHoursService.isMarketOpen()).thenReturn(false);
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client));
        when(instrumentMapper.findById(2L)).thenReturn(Optional.of(instrument));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100.00"));
        when(orderStatusMapper.findByName("Queued")).thenReturn(Optional.of(status(4L, "Queued")));
        if (!buy) {
            when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L))
                    .thenReturn(Optional.of(new HoldingEntity(9L, client, instrument,
                            new BigDecimal("5"), new BigDecimal("80"))));
        }
        doAnswer(invocation -> {
            invocation.getArgument(0, OrderEntity.class).setId(20L);
            return null;
        }).when(orderMapper).save(any(OrderEntity.class));
        var response = buy
                ? tradeService.buyStock(new BuyOrderRequestDTO(1L, 2L, new BigDecimal("2")))
                : tradeService.sellStock(new SellOrderRequestDTO(1L, 2L, new BigDecimal("2")));
        assertEquals("QUEUED", response.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(response.getTotalAmount());
        org.junit.jupiter.api.Assertions.assertNull(response.getPricePerShare());
        assertEquals(1000, client.getCashBalance());
        verify(clientMapper, never()).update(any());
        verify(holdingMapper, never()).save(any());
        verify(holdingMapper, never()).update(any());
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getTimeFilled());
        return captor.getValue();
    }

    @Test
    void closedSessionBuyPersistsWithoutSettlement() {
        queueOrder(true);
    }

    @Test
    void closedSessionSellPersistsWithoutSettlement() {
        queueOrder(false);
    }

    @Test
    void queuedOrderRevalidatesAtNewPriceAndUpdatesOriginalOrder() {
        OrderEntity queued = queueOrder(true);
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(orderMapper.lockQueued(20L)).thenReturn(20L, (Long) null);
        when(orderMapper.findById(20L)).thenReturn(queued);
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("120"));
        when(orderStatusMapper.findById(2L)).thenReturn(Optional.of(filledStatus));
        when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.empty());
        tradeService.executeQueued(20L);
        tradeService.executeQueued(20L);
        assertEquals(760, client.getCashBalance());
        assertEquals("Filled", queued.getStatus().getName());
        assertEquals(new BigDecimal("120"), queued.getPurchasePrice());
        assertNotNull(queued.getTimeFilled());
        verify(orderMapper).update(queued);
        verify(orderMapper).save(any(OrderEntity.class));
        verify(clientMapper).update(client);
    }

    @Test
    void queuedOrderRejectsIfCashIsNoLongerSufficient() {
        OrderEntity queued = queueOrder(true);
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(orderMapper.lockQueued(20L)).thenReturn(20L);
        when(orderMapper.findById(20L)).thenReturn(queued);
        when(orderStatusMapper.findByName("Rejected")).thenReturn(Optional.of(status(5L, "Rejected")));
        client.setCashBalance(0);
        tradeService.executeQueued(20L);
        assertEquals("Rejected", queued.getStatus().getName());
        verify(clientMapper, never()).update(any());
        verify(notificationService).publish(20L, 1L, "AAPL", "BUY", "REJECTED",
                "Insufficient cash balance. Required: $200.00000, Available: $0.0");
    }

    @Test
    void queuedOrderWaitsWhenPriceIsUnavailable() {
        OrderEntity queued = queueOrder(true);
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(orderMapper.lockQueued(20L)).thenReturn(20L);
        when(orderMapper.findById(20L)).thenReturn(queued);
        when(priceService.getCurrentPrice("AAPL")).thenReturn(null);
        assertThrows(com.sleapy.project.exceptions.OrderValidationException.class,
                () -> tradeService.executeQueued(20L));
        assertEquals("Queued", queued.getStatus().getName());
        verify(orderMapper, never()).update(any());
        verify(clientMapper, never()).update(any());
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(marketHoursService.isMarketOpen()).thenReturn(true);
        var validationService = new OrderBusinessRuleValidationService(
                clientMapper, instrumentMapper, holdingMapper, marketPriceMapper, priceService, marketHoursService);
        tradeService = new TradeService(validationService, clientMapper, holdingMapper, orderMapper,
                orderStatusMapper, notificationService, marketHoursService);
        client = new ClientEntity();
        client.setId(1L);
        client.setCashBalance(1000.00);
        client.setClientStatus(new ClientStatus(1L, "Active"));

        instrument = new InstrumentEntity();
        instrument.setId(2L);
        instrument.setSymbol("AAPL");
        instrument.setSymbolName("Apple Inc.");

        filledStatus = new OrderStatus();
        filledStatus.setId(2L);
        filledStatus.setName("Filled");

    }

    private void stubQuoteAndSettlementDependencies() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client));
        when(instrumentMapper.findById(2L)).thenReturn(Optional.of(instrument));
        when(orderStatusMapper.findById(2L)).thenReturn(Optional.of(filledStatus));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100.00"));
    }

    @Test
    void buySettlesCashHoldingAndPermanentHistoryAtExecutionPrice() {
        stubQuoteAndSettlementDependencies();
        when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            invocation.getArgument(0, OrderEntity.class).setId(10L);
            return null;
        }).when(orderMapper).save(any(OrderEntity.class));

        var response = tradeService.buyStock(new BuyOrderRequestDTO(1L, 2L, new BigDecimal("2")));

        assertEquals(0, new BigDecimal("200.00").compareTo(response.getTotalAmount()));
        assertEquals(new BigDecimal("100.00"), response.getPricePerShare());
        assertEquals("FILLED", response.getStatus());
        assertEquals(10L, response.getOrderId());
        assertEquals(800.00, client.getCashBalance(), 0.001);

        ArgumentCaptor<HoldingEntity> holdingCaptor = ArgumentCaptor.forClass(HoldingEntity.class);
        verify(holdingMapper).save(holdingCaptor.capture());
        assertEquals(new BigDecimal("2.000"), holdingCaptor.getValue().getQuantityShares());
        assertEquals(new BigDecimal("100.00"), holdingCaptor.getValue().getPurchasePrice());

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals("BUY", savedOrder.getTradeType());
        assertEquals(new BigDecimal("100.00"), savedOrder.getPurchasePrice());
        assertEquals(filledStatus, savedOrder.getStatus());
        assertNotNull(savedOrder.getTimeFilled());
        assertEquals(LocalDate.now(java.time.ZoneId.of("America/New_York")), savedOrder.getTimeFilled());
    }

    @Test
    void sellSettlesCashHoldingAndPermanentHistoryAtExecutionPrice() {
        stubQuoteAndSettlementDependencies();
        client.setCashBalance(500.00);
        HoldingEntity holding = new HoldingEntity(9L, client, instrument, new BigDecimal("5"), new BigDecimal("80.00"));
        when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.of(holding));
        doAnswer(invocation -> {
            invocation.getArgument(0, OrderEntity.class).setId(11L);
            return null;
        }).when(orderMapper).save(any(OrderEntity.class));

        var response = tradeService.sellStock(new SellOrderRequestDTO(1L, 2L, new BigDecimal("2")));

        assertEquals(0, new BigDecimal("200.00").compareTo(response.getTotalAmount()));
        assertEquals(700.00, client.getCashBalance(), 0.001);
        assertEquals(new BigDecimal("3.000"), holding.getQuantityShares());
        verify(holdingMapper).update(holding);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals("SELL", savedOrder.getTradeType());
        assertEquals(new BigDecimal("100.00"), savedOrder.getPurchasePrice());
        assertNotNull(savedOrder.getTimeFilled());
    }

    @Test
    void insufficientCashRejectsBeforeSettlementMutation() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client));
        when(instrumentMapper.findById(2L)).thenReturn(Optional.of(instrument));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100.00"));
        client.setCashBalance(50.00);

        assertThrows(com.sleapy.project.exceptions.OrderValidationException.class,
                () -> tradeService.buyStock(new BuyOrderRequestDTO(1L, 2L, new BigDecimal("2"))));

        verify(clientMapper, never()).update(any(ClientEntity.class));
        verify(holdingMapper, never()).save(any(HoldingEntity.class));
        verify(holdingMapper, never()).update(any(HoldingEntity.class));
        verify(orderMapper, never()).save(any(OrderEntity.class));
    }
}
