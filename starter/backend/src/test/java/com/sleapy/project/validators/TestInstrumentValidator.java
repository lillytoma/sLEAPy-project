package com.sleapy.project.validators;

import com.sleapy.project.exceptions.InvalidInstrumentFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Instrument Validator Tests")
public class TestInstrumentValidator {

    private InstrumentValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new InstrumentValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid symbol")
    public void testValidSymbol() {
        assertDoesNotThrow(() -> validator.validateInstrumentSymbol("AAPL"));
        assertDoesNotThrow(() -> validator.validateInstrumentSymbol("MSFT"));
        assertDoesNotThrow(() -> validator.validateInstrumentSymbol("BRK"));
    }

    @Test
    @DisplayName("Should fail validation for symbol with lowercase letters")
    public void testLowercaseSymbol() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("aapl")
        );
        assertTrue(exception.getMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Should fail validation for symbol shorter than 2 characters")
    public void testSymbolTooShort() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("A")
        );
        assertTrue(exception.getMessage().contains("between 2 and 5"));
    }

    @Test
    @DisplayName("Should fail validation for symbol longer than 5 characters")
    public void testSymbolTooLong() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("TOOLONG")
        );
        assertTrue(exception.getMessage().contains("between 2 and 5"));
    }

    @Test
    @DisplayName("Should fail validation for null symbol")
    public void testNullSymbol() {
        assertThrows(
            NullPointerException.class,
            () -> validator.validateInstrumentSymbol(null)
        );
    }

    @Test
    @DisplayName("Should fail validation for empty symbol")
    public void testEmptySymbol() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("")
        );
        assertTrue(exception.getMessage().contains("Instrument symbol cannot be empty"));
    }

    @Test
    @DisplayName("Should fail validation for symbol with numbers")
    public void testSymbolWithNumbers() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("AA1L")
        );
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Should fail validation for symbol with special characters")
    public void testSymbolWithSpecialCharacters() {
        InvalidInstrumentFormatException exception = assertThrows(
            InvalidInstrumentFormatException.class,
            () -> validator.validateInstrumentSymbol("AA-L")
        );
        assertNotNull(exception);
    }

    @Test
    @DisplayName("Should pass validation for 2 character symbol")
    public void testMinLengthSymbol() {
        assertDoesNotThrow(() -> validator.validateInstrumentSymbol("AA"));
    }

    @Test
    @DisplayName("Should pass validation for 5 character symbol")
    public void testMaxLengthSymbol() {
        assertDoesNotThrow(() -> validator.validateInstrumentSymbol("AAAAA"));
    }
}