package com.sleapy.project.validators;

import com.sleapy.project.exceptions.InvalidInstrumentFormatException;
import org.springframework.stereotype.Component;

@Component
public class InstrumentValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 5;
    private static final String UPPERCASE_PATTERN = "^[A-Z]{2,5}$";

    /**
     * Validates instrument symbol format
     * @param instrumentSymbol the symbol to validate
     * @throws InvalidInstrumentFormatException if symbol is invalid
     * @throws NullPointerException if symbol is null
     */
    public void validateInstrumentSymbol(String instrumentSymbol) throws InvalidInstrumentFormatException {

        // Check null
        if (instrumentSymbol == null) {
            throw new NullPointerException("Instrument symbol cannot be null");
        }

        // Check empty
        if (instrumentSymbol.isEmpty()) {
            throw new InvalidInstrumentFormatException("Instrument symbol cannot be empty");
        }

        // Check length (this catches empty strings too)
        if (instrumentSymbol.length() < MIN_LENGTH || instrumentSymbol.length() > MAX_LENGTH) {
            throw new InvalidInstrumentFormatException("Instrument symbol is not between 2 and 5 characters");
        }

        // Check if uppercase letters only
        if (!instrumentSymbol.matches(UPPERCASE_PATTERN)) {
            throw new InvalidInstrumentFormatException("Instrument symbol must be uppercase letters only (e.g., AAPL)");
        }
    }

    /**
     * Check if symbol is valid without throwing exception
     */
    public boolean isValidInstrumentSymbol(String instrumentSymbol) {
        try {
            validateInstrumentSymbol(instrumentSymbol);
            return true;
        } catch (InvalidInstrumentFormatException | NullPointerException e) {
            return false;
        }
    }
}