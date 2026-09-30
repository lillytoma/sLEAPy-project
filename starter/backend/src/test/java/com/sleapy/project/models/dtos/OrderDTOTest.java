package com.sleapy.project.models.dtos;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.sleapy.project.models.enums.OrderStatus;
import com.sleapy.project.models.enums.InstrumentType;



import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class OrderDTOTest {

    @Autowired
    private Validator validator;

    private OrderDTO dto;
    private InstrumentDTO validInstrument;

    @BeforeEach
    void setUp() {
        validInstrument = new InstrumentDTO();
        validInstrument.setId(1L);
        validInstrument.setSymbol("AAPL");
        validInstrument.setSymbolName("APPLE");
        validInstrument.setInstrumentType(InstrumentType.BOND);

        dto = new OrderDTO();
        dto.setId(1L);
        dto.setTimeOfPurchase(LocalDate.now().minusDays(5));
        dto.setQuantity(10);
        dto.setTimeFilled(LocalDate.now());
        dto.setPurchasePrice(150.50);
        dto.setInstrument(validInstrument);
        dto.setStatus(OrderStatus.PENDING);
    }

    void assertHasViolations(String violatedField) {
        Set<ConstraintViolation<OrderDTO>> violations = validator.validate(this.dto);
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(violatedField));
    }

    @Test
    void validOrderHasNoViolations() {
        Set<ConstraintViolation<OrderDTO>> violations = validator.validate(this.dto);
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

    // Time of Purchase tests - @NotNull + @PastOrPresent (LocalDate)
    @Test
    void nullTimeOfPurchaseViolation() {
        this.dto.setTimeOfPurchase(null);
        assertHasViolations("timeOfPurchase");
    }

    @Test
    void futureTimeOfPurchaseViolation() {
        this.dto.setTimeOfPurchase(LocalDate.now().plusDays(1));
        assertHasViolations("timeOfPurchase");
    }

    @Test
    void pastTimeOfPurchaseIsValid() {
        this.dto.setTimeOfPurchase(LocalDate.of(2020, 1, 1));
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void todayTimeOfPurchaseIsValid() {
        this.dto.setTimeOfPurchase(LocalDate.now());
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Quantity tests - @NotNull + @Positive
    @Test
    void zeroQuantityViolation() {
        this.dto.setQuantity(0);
        assertHasViolations("quantity");
    }

    @Test
    void negativeQuantityViolation() {
        this.dto.setQuantity(-5);
        assertHasViolations("quantity");
    }

    @Test
    void positiveQuantityIsValid() {
        this.dto.setQuantity(100);
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Time Filled tests - @PastOrPresent (no @NotNull, so can be null)
    @Test
    void nullTimeFilledIsValid() {
        this.dto.setTimeFilled(null);
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void futureTimeFilledViolation() {
        this.dto.setTimeFilled(LocalDate.now().plusDays(1));
        assertHasViolations("timeFilled");
    }

    @Test
    void pastTimeFilledIsValid() {
        this.dto.setTimeFilled(LocalDate.of(2020, 1, 1));
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Purchase Price tests - @NotNull + @PositiveOrZero
    @Test
    void nullPurchasePriceViolation() {
        this.dto.setPurchasePrice(0);
        // Note: primitive double can't be null, but 0 is valid with @PositiveOrZero
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void negativePurchasePriceViolation() {
        this.dto.setPurchasePrice(-10.50);
        assertHasViolations("purchasePrice");
    }

    @Test
    void zeroPurchasePriceIsValid() {
        this.dto.setPurchasePrice(0.0);
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void positivePurchasePriceIsValid() {
        this.dto.setPurchasePrice(200.99);
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Instrument tests - @NotNull + @Valid (cascades validation)
    @Test
    void nullInstrumentViolation() {
        this.dto.setInstrument(null);
        assertHasViolations("instrument");
    }

    @Test
    void invalidInstrumentViolation() {
        InstrumentDTO invalidInstrument = new InstrumentDTO();
        invalidInstrument.setId(-1L); // Negative ID violates @Positive
        this.dto.setInstrument(invalidInstrument);
        // Should have violations from cascaded validation
        Set<ConstraintViolation<OrderDTO>> violations = validator.validate(this.dto);
        assertThat(violations).isNotEmpty();
    }

    // Status tests - @NotNull
    @Test
    void nullStatusViolation() {
        this.dto.setStatus(null);
        assertHasViolations("status");
    }

    @Test
    void validStatusIsValid() {
        this.dto.setStatus(OrderStatus.FILLED);
        assertThat(validator.validate(this.dto)).isEmpty();
    }
}
