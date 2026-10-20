package com.sleapy.project.services;

import com.sleapy.project.exceptions.OrderValidationException;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.MarketPriceMapper;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.ClientStatus;
import com.sleapy.project.models.entities.InstrumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderBusinessRuleValidationServiceTest {
    @Mock private ClientMapper clientMapper;
    @Mock private InstrumentMapper instrumentMapper;
    @Mock private HoldingMapper holdingMapper;
    @Mock private MarketPriceMapper marketPriceMapper;
    @Mock private PriceService priceService;
    @Mock private MarketHoursService marketHoursService;

    private OrderBusinessRuleValidationService validationService;
    private ClientEntity client;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        validationService = new OrderBusinessRuleValidationService(
            clientMapper, instrumentMapper, holdingMapper, marketPriceMapper, priceService, marketHoursService);
        client = new ClientEntity();
        client.setCashBalance(100);
        client.setClientStatus(new ClientStatus(1L, "Active"));
        instrument = new InstrumentEntity();
        instrument.setSymbol("ABC");
        instrument.setCurrentPrice(BigDecimal.TEN);
        lenient().when(clientMapper.findById(1L)).thenReturn(Optional.of(client));
        lenient().when(instrumentMapper.findById(2L)).thenReturn(Optional.of(instrument));
        lenient().when(marketHoursService.isMarketOpen()).thenReturn(true);
    }

    @Test
    void rejectsBuyWhenCashBalanceIsInsufficient() {
        when(priceService.getCurrentPrice("ABC")).thenThrow(new UnsupportedOperationException());

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validateBuy(1L, 2L, BigDecimal.valueOf(11)));

        assertEquals("INSUFFICIENT_CASH", exception.getRule());
    }

    @Test
    void roundsFractionalQuantityToThreeDecimalPlaces() {
        when(priceService.getCurrentPrice("ABC")).thenThrow(new UnsupportedOperationException());

        OrderBusinessRuleValidationService.BuyValidation validation =
                validationService.validateBuy(1L, 2L, BigDecimal.valueOf(0.1236));

        assertEquals(BigDecimal.valueOf(0.124), validation.quantity());
    }

    @Test
    void rejectsSellWhenInstrumentIsNotOwned() {
        when(holdingMapper.findByClientIdAndInstrumentId(1L, 2L)).thenReturn(Optional.empty());

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validateSell(1L, 2L, BigDecimal.ONE));

        assertEquals("POSITION_NOT_OWNED", exception.getRule());
    }

    @Test
    void acceptsOrdersWhenMarketIsClosedForTesting() {
        assertEquals(0, BigDecimal.ONE.compareTo(validationService.validateBuy(1L, 2L, BigDecimal.ONE).quantity()));
    }
}