package com.sleapy.project.models.dtos;

import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sleapy.project.models.enums.InstrumentType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class HoldingDTOTest {

    @Autowired
    private Validator validator;

    private HoldingDTO dto;

    private static ClientDTO createValidClient() {
        ClientDTO client = new ClientDTO();
        client.setId(1L);
        client.setEmail("john.doe@example.com");
        client.setUsername("johndoe12");
        client.setCashBalance(10000);
        client.setPhoneNumber("5551234567");
        client.setSsnLast4("5678");
        client.setFullAddress("123 Main St New York, NY 10001 United States");
        client.setBirthDate("1990-01-15");
        client.setHoldings(new ArrayList<>());
        return client;
    }

    private static InstrumentDTO createValidInstrument() {
        InstrumentDTO instrument = new InstrumentDTO();
        instrument.setId(1L);
        instrument.setSymbol("AAPL");
        instrument.setSymbolName("APPLE");
        instrument.setInstrumentType(InstrumentType.CRYTPO);
        return instrument;
    }

    @BeforeEach
    void setUp() {
        dto = new HoldingDTO();
        dto.setId(1L);
        dto.setInstrument(createValidInstrument());
        dto.setClient(createValidClient());
        dto.setTotalShares(100.0);
        dto.setTotalPrice(15000.50);
        dto.setClient(createValidClient());
        dto.setInstrument(createValidInstrument());
    }

    void assertHasViolations(String violatedField) {
        Set<ConstraintViolation<HoldingDTO>> violations = validator.validate(this.dto);
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(violatedField));
    }

    @Test
    void validHoldingHasNoViolations() {
        Set<ConstraintViolation<HoldingDTO>> violations = validator.validate(this.dto);
        assertThat(violations).isEmpty();
    }

    // ID tests - @NotNull + @Positive
    @Test
    void nullIdViolation() {
        this.dto.setId(null);
        assertHasViolations("id");
    }

    @Test
    void zeroIdViolation() {
        this.dto.setId(0L);
        assertHasViolations("id");
    }

    @Test
    void negativeIdViolation() {
        this.dto.setId(-1L);
        assertHasViolations("id");
    }

    // Instrument tests - @NotNull + @Valid (cascades validation)
    @Test
    void nullInstrumentViolation() {
        this.dto.setInstrument(null);
        assertHasViolations("instrument");
    }

    // Client tests - @NotNull + @Valid (cascades validation)
    @Test
    void nullClientViolation() {
        this.dto.setClient(null);
        assertHasViolations("client");
    }

    // Total Shares tests - @PositiveOrZero
    @Test
    void negativeTotalSharesViolation() {
        this.dto.setTotalShares(-10.5);
        assertHasViolations("totalShares");
    }

    @Test
    void zeroTotalSharesIsValid() {
        this.dto.setTotalShares(0.0);
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Total Price tests - @PositiveOrZero
    @Test
    void negativeTotalPriceViolation() {
        this.dto.setTotalPrice(-500.25);
        assertHasViolations("totalPrice");
    }

    @Test
    void zeroTotalPriceIsValid() {
        this.dto.setTotalPrice(0.0);
        assertThat(validator.validate(this.dto)).isEmpty();
    }
}
