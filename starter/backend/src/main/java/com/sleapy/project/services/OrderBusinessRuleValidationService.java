package com.sleapy.project.services;

import com.sleapy.project.exceptions.OrderValidationException;
import com.sleapy.project.exceptions.InsufficientCashException;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.mappers.InstrumentMapper;
import com.sleapy.project.mappers.MarketPriceMapper;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.entities.InstrumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBusinessRuleValidationService {
    private final ClientMapper clientMapper;
    private final InstrumentMapper instrumentMapper;
    private final HoldingMapper holdingMapper;
    private final MarketPriceMapper marketPriceMapper;
    private final PriceService priceService;
    private final MarketHoursService marketHoursService;

    public BuyValidation validateBuy(Long clientId, Long instrumentId, BigDecimal quantity) {
        BigDecimal normalizedQuantity = normalizeQuantity(quantity);
        validateMarketHours();
        ClientEntity client = findActiveClient(clientId);
        InstrumentEntity instrument = findInstrument(instrumentId);
        BigDecimal price = resolveCurrentPrice(instrument);
        BigDecimal total = price.multiply(normalizedQuantity);
        BigDecimal cash = BigDecimal.valueOf(client.getCashBalance());

        if (cash.compareTo(total) < 0) {
            throw new InsufficientCashException("Insufficient cash balance. Required: $" + total
                    + ", Available: $" + cash);
        }

        log.info("Order validation accepted: clientId={}, instrumentId={}, side=BUY, quantity={}",
                clientId, instrumentId, normalizedQuantity);
            return new BuyValidation(client, instrument, normalizedQuantity, price, total);
    }

    public SellValidation validateSell(Long clientId, Long instrumentId, BigDecimal quantity) {
            BigDecimal normalizedQuantity = normalizeQuantity(quantity);
        validateMarketHours();
        ClientEntity client = findActiveClient(clientId);
        InstrumentEntity instrument = findInstrument(instrumentId);
        HoldingEntity holding = holdingMapper.findByClientIdAndInstrumentId(clientId, instrumentId)
                .orElseThrow(() -> reject("POSITION_NOT_OWNED", "Client does not own " + instrument.getSymbol()));

        BigDecimal owned = holding.getQuantityShares();
        if (owned == null || owned.compareTo(normalizedQuantity) < 0) {
            throw reject("INSUFFICIENT_SHARES", "Insufficient shares. Owned: "
                + (owned == null ? BigDecimal.ZERO : owned) + ", Attempting to sell: " + normalizedQuantity);
        }

        BigDecimal price = resolveCurrentPrice(instrument);
        log.info("Order validation accepted: clientId={}, instrumentId={}, side=SELL, quantity={}",
                clientId, instrumentId, normalizedQuantity);
            return new SellValidation(client, instrument, holding, normalizedQuantity, price, price.multiply(normalizedQuantity));
    }

    private ClientEntity findActiveClient(Long clientId) {
        ClientEntity client = clientMapper.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + clientId));
        if (client.getClientStatus() == null || client.getClientStatus().getName() == null
                || !"active".equalsIgnoreCase(client.getClientStatus().getName())) {
            throw reject("CLIENT_NOT_ACTIVE", "Client is not active and cannot trade");
        }
        return client;
    }

    private InstrumentEntity findInstrument(Long instrumentId) {
        return instrumentMapper.findById(instrumentId)
                .orElseThrow(() -> new NoSuchElementException("Instrument not found with ID: " + instrumentId));
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw reject("INVALID_QUANTITY", "Quantity must be positive");
        }
        BigDecimal normalized = quantity.setScale(3, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw reject("INVALID_QUANTITY", "Quantity must round to at least 0.001 shares");
        }
        return normalized;
    }

    private void validateMarketHours() {
        if (!marketHoursService.isMarketOpen()) {
            throw reject("MARKET_CLOSED", "Trading is unavailable outside US market hours (09:30-16:00 Eastern, Monday-Friday)");
        }
    }

    private OrderValidationException reject(String rule, String message) {
        log.warn("Order validation rejected: rule={}, message={}", rule, message);
        return new OrderValidationException(rule, message);
    }

    private BigDecimal resolveCurrentPrice(InstrumentEntity instrument) {
        BigDecimal price;
        try {
            price = priceService.getCurrentPrice(instrument.getSymbol());
        } catch (UnsupportedOperationException ex) {
            price = null;
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            price = marketPriceMapper.findBySymbol(instrument.getSymbol())
                    .map(marketPrice -> marketPrice.getCurrentPrice()).orElse(instrument.getCurrentPrice());
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw reject("INVALID_PRICE", "Invalid price for instrument: " + instrument.getSymbol());
        }
        return price;
    }

    public record BuyValidation(ClientEntity client, InstrumentEntity instrument,
                                BigDecimal quantity, BigDecimal price, BigDecimal total) {}

    public record SellValidation(ClientEntity client, InstrumentEntity instrument,
                                 HoldingEntity holding, BigDecimal quantity, BigDecimal price, BigDecimal total) {}
}