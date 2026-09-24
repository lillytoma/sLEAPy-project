package com.sleapy.project.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sleapy.project.exceptions.InvalidEmailFormatException;

public class ClientValidatorTest {

    private final ClientValidator uut = new ClientValidator();

    @Test 
    void shouldRejectAnEmptyString(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail(""));
    }

    @Test
    void shouldRejectANullValue(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail(null));
    }

    @Test
    void shouldRejectABlankString(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail("   "));
    }

    @Test
    void shouldRejectEmailWithoutAtSymbol(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail("jane.example.com"));
    }

    @Test
    void shouldRejectEmailWithoutDomain(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail("jane@"));
    }

    @Test
    void shouldRejectEmailWithoutTopLevelDomain(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail("jane@example"));
    }

    @Test
    void shouldRejectEmailWithWhitespaceInLocalPart(){
        assertThrows(InvalidEmailFormatException.class, () -> uut.validateEmail("jane doe@example.com"));
    }

    @Test
    void shouldAcceptASimpleValidEmail(){
        assertDoesNotThrow(() -> uut.validateEmail("jane@example.com"));
    }

    @Test
    void shouldAcceptAValidEmailWithSubdomainAndAlias(){
        assertDoesNotThrow(() -> uut.validateEmail("jane.doe+alerts@mail.example.co.uk"));
    }
}
