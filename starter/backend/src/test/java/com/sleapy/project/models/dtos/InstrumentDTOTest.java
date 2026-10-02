package com.sleapy.project.models.dtos;

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
public class InstrumentDTOTest {

    @Autowired
    private Validator validator;

    private InstrumentDTO dto;

    @BeforeEach
    void setUp() {
        dto = new InstrumentDTO();
        dto.setId(1L);
        dto.setSymbol("AAPL");
        dto.setSymbolName("APPLE");
        dto.setInstrumentType(InstrumentType.BOND);
    }

    void assertHasViolations(String violatedField) {
        Set<ConstraintViolation<InstrumentDTO>> violations = validator.validate(this.dto);
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(violatedField));
    }

    @Test
    void validInstrumentHasNoViolations() {
        Set<ConstraintViolation<InstrumentDTO>> violations = validator.validate(this.dto);
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

    // Symbol tests - @NotBlank
    @Test
    void nullSymbolViolation() {
        this.dto.setSymbol(null);
        assertHasViolations("symbol");
    }

    @Test
    void emptySymbolViolation() {
        this.dto.setSymbol("");
        assertHasViolations("symbol");
    }

    @Test
    void blankSymbolViolation() {
        this.dto.setSymbol("   ");
        assertHasViolations("symbol");
    }

    @Test
    void validSymbolIsValid() {
        this.dto.setSymbol("IBM");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Symbol Name tests - @NotBlank + @Size(2-5) + @Pattern(uppercase only)
    @Test
    void nullSymbolNameViolation() {
        this.dto.setSymbolName(null);
        assertHasViolations("symbolName");
    }

    @Test
    void emptySymbolNameViolation() {
        this.dto.setSymbolName("");
        assertHasViolations("symbolName");
    }

    @Test
    void blankSymbolNameViolation() {
        this.dto.setSymbolName("   ");
        assertHasViolations("symbolName");
    }

    @Test
    void symbolNameTooShortViolation() {
        this.dto.setSymbolName("A");
        assertHasViolations("symbolName");
    }

    @Test
    void symbolNameTooLongViolation() {
        this.dto.setSymbolName("TOOLONG");
        assertHasViolations("symbolName");
    }

    @Test
    void symbolNameWithLowercaseViolation() {
        this.dto.setSymbolName("Apple");
        assertHasViolations("symbolName");
    }

    @Test
    void symbolNameWithNumbersViolation() {
        this.dto.setSymbolName("AP123");
        assertHasViolations("symbolName");
    }

    @Test
    void symbolNameWithSpecialCharsViolation() {
        this.dto.setSymbolName("AP@LE");
        assertHasViolations("symbolName");
    }

    @Test
    void validSymbolNameMinLengthIsValid() {
        this.dto.setSymbolName("AB");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void validSymbolNameMaxLengthIsValid() {
        this.dto.setSymbolName("ABCDE");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void validSymbolNameMidRangeIsValid() {
        this.dto.setSymbolName("MSFT");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Instrument Type tests - @NotNull
    @Test
    void nullInstrumentTypeViolation() {
        this.dto.setInstrumentType(null);
        assertHasViolations("instrumentType");
    }

    @Test
    void validInstrumentTypeIsValid() {
        this.dto.setInstrumentType(InstrumentType.BOND);
        assertThat(validator.validate(this.dto)).isEmpty();
    }
}
