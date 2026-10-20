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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sleapy.project.exceptions.InsufficientCashException;
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
    private OrderBusinessRuleValidationService orderValidationService;

    @InjectMocks
    private TradeService tradeService;

    private ClientEntity client;
    private InstrumentEntity instrument;
    private HoldingEntity holding;
    private OrderStatus filledStatus;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);
        client.setCashBalance(1000.00);
        client.setClientStatus(new ClientStatus(1L, "Active"));

        instrument = new InstrumentEntity();
        instrument.setId(2L);
        instrument.setSymbol("AAPL");
        instrument.setSymbolName("Apple Inc.");

        holding = new HoldingEntity(9L, client, instrument, new BigDecimal("5"), new BigDecimal("80.00"));

        filledStatus = new OrderStatus();
        filledStatus.setId(2L);
        filledStatus.setName("Filled");

    }

    private void stubBuySettlementDependencies() {
        when(orderStatusMapper.findById(2L)).thenReturn(Optional.of(filledStatus));
        when(orderValidationService.validateBuy(1L, 2L, new BigDecimal("2")))
            .thenReturn(new OrderBusinessRuleValidationService.BuyValidation(
                client, instrument, new BigDecimal("2"), new BigDecimal("100.00"), new BigDecimal("200.00")));
    }

    private void stubSellSettlementDependencies() {
        when(orderStatusMapper.findById(2L)).thenReturn(Optional.of(filledStatus));
        when(orderValidationService.validateSell(1L, 2L, new BigDecimal("2")))
            .thenReturn(new OrderBusinessRuleValidationService.SellValidation(
                client, instrument, holding, new BigDecimal("2"),
                new BigDecimal("100.00"), new BigDecimal("200.00")));
    }

    @Test
    void buySettlesCashHoldingAndPermanentHistoryAtExecutionPrice() {
        stubBuySettlementDependencies();
        when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            invocation.getArgument(0, OrderEntity.class).setId(10L);
            return null;
        }).when(orderMapper).save(any(OrderEntity.class));

        var response = tradeService.buyStock(new BuyOrderRequestDTO(1L, 2L, new BigDecimal("2")));

        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("100.00"), response.getPricePerShare());
        assertEquals("FILLED", response.getStatus());
        assertEquals(10L, response.getOrderId());
        assertEquals(800.00, client.getCashBalance(), 0.001);

        ArgumentCaptor<HoldingEntity> holdingCaptor = ArgumentCaptor.forClass(HoldingEntity.class);
        verify(holdingMapper).save(holdingCaptor.capture());
        assertEquals(new BigDecimal("2"), holdingCaptor.getValue().getQuantityShares());
        assertEquals(new BigDecimal("100.00"), holdingCaptor.getValue().getPurchasePrice());

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderMapper).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertEquals("BUY", savedOrder.getTradeType());
        assertEquals(new BigDecimal("100.00"), savedOrder.getPurchasePrice());
        assertEquals(filledStatus, savedOrder.getStatus());
        assertNotNull(savedOrder.getTimeFilled());
        assertEquals(LocalDate.now(), savedOrder.getTimeFilled());
    }

    @Test
    void sellSettlesCashHoldingAndPermanentHistoryAtExecutionPrice() {
        stubSellSettlementDependencies();
        client.setCashBalance(500.00);
        doAnswer(invocation -> {
            invocation.getArgument(0, OrderEntity.class).setId(11L);
            return null;
        }).when(orderMapper).save(any(OrderEntity.class));

        var response = tradeService.sellStock(new SellOrderRequestDTO(1L, 2L, new BigDecimal("2")));

        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());
        assertEquals(700.00, client.getCashBalance(), 0.001);
        assertEquals(new BigDecimal("3"), holding.getQuantityShares());
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
        client.setCashBalance(50.00);
        when(orderValidationService.validateBuy(1L, 2L, new BigDecimal("2")))
            .thenThrow(new InsufficientCashException("Insufficient cash balance"));

        assertThrows(InsufficientCashException.class,
                () -> tradeService.buyStock(new BuyOrderRequestDTO(1L, 2L, new BigDecimal("2"))));

        verify(clientMapper, never()).update(any(ClientEntity.class));
        verify(holdingMapper, never()).save(any(HoldingEntity.class));
        verify(holdingMapper, never()).update(any(HoldingEntity.class));
        verify(orderMapper, never()).save(any(OrderEntity.class));
    }
}
