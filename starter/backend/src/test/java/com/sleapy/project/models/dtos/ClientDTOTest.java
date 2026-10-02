package com.sleapy.project.models.dtos;

import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class ClientDTOTest {
    
    @Autowired 
    private Validator validator;

    private ClientDTO dto;

    @BeforeEach 
    void setUp() {
        dto = new ClientDTO();

        dto.setId(1L);
        dto.setEmail("jane.doe@example.com");
        dto.setUsername("janedoe12");
        dto.setCashBalance(7500);
        dto.setPhoneNumber("1234567890");
        dto.setSsnLast4("1234");
        dto.setFullAddress("1234 Maple Street Westlake, TX 12345 United States");
        dto.setBirthDate("1999-01-01");
        dto.setHoldings(new ArrayList<>());

    }

    void assertHasViolations(String violatedField) {
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(this.dto);
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(violatedField));
    }

    @Test
    void validClientHasNoViolations() {
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(this.dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void blankIdViolation() {
        this.dto.setId(null);
        assertHasViolations("id");
    }

    @Test 
    void zeroValueIdViolation() {
        this.dto.setId(0L);
        assertHasViolations("id");
    }

    @Test 
    void negativeValueIdViolation() {
        this.dto.setId(-1L);
        assertHasViolations("id");
    }

    @Test 
    void blankEmailViolation() {
        this.dto.setEmail(null);
        assertHasViolations("email");
    }

    @Test 
    void emptyStringEmailViolation() {
        this.dto.setEmail("");
        assertHasViolations("email");
    }

    @Test 
    void emailWithoutAtSymbolViolation() {
        this.dto.setEmail("janedoeemail.com");
        assertHasViolations("email");
    }

    @Test 
    void emailWithoutDomainViolation() {
        this.dto.setEmail("jane.doe@");
        assertHasViolations("email");
    }

    @Test 
    void emailWithoutLocalPartViolation() {
        this.dto.setEmail("@example.com");
        assertHasViolations("email");
    }

    @Test 
    void emailWithSpacesViolation() {
        this.dto.setEmail("jane doe@example.com");
        assertHasViolations("email");
    }

    @Test 
    void emailWithValidFormatIsValid() {
        this.dto.setEmail("john.smith+tag@company.co.uk");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void blankUsernameViolation() {
        this.dto.setUsername(null);
        assertHasViolations("username");
    }

    @Test
    void emptyStringUsernameViolation() {
        this.dto.setUsername("");
        assertHasViolations("username");
    }

    @Test
    void largeUsernameViolation() { // max username length is currently 25 
        this.dto.setUsername("validusername1234567890abc"); // 26 chars
        assertHasViolations("username");
    }

    @Test 
    void negativeCashBalanceViolation() {
        this.dto.setCashBalance(-1);
        assertHasViolations("cashBalance");
    }

    @Test 
    void blankPhoneNumberIsValid() {
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(this.dto);
        assertThat(violations).isEmpty();
    }

    @Test 
    void phoneNumberLessThan10DigitsViolation() {
        this.dto.setPhoneNumber("");
        assertHasViolations("phoneNumber");
    }

    @Test 
    void phoneNumberMoreThan10DigitsViolation() {
        this.dto.setPhoneNumber("12345678910");
        assertHasViolations("phoneNumber");
    }

    @Test 
    void blankSSNValid() {
        this.dto.setSsnLast4(null);
        assertHasViolations("ssnLast4");
    }

    @Test 
    void ssnLessThan4DigitsViolation() {
        this.dto.setSsnLast4("");
        assertHasViolations("ssnLast4");
    }

    @Test 
    void ssnMoreThan4DigitsViolation() {
        this.dto.setSsnLast4("12345");
        assertHasViolations("ssnLast4");
    }

    @Test 
    void nullBirthDateViolation() {
        this.dto.setBirthDate(null);
        assertHasViolations("birthDate");
    }

    @Test 
    void emptyBirthDateViolation() {
        this.dto.setBirthDate("");
        assertHasViolations("birthDate");
    }

    @Test 
    void whitespaceBirthDateViolation() {
        this.dto.setBirthDate("   ");
        assertHasViolations("birthDate");
    }

    @Test 
    void birthDateWithInvalidFormatMMDDYYYYViolation() {
        this.dto.setBirthDate("05-15-1990"); // MM-DD-YYYY instead of YYYY-MM-DD
        assertHasViolations("birthDate");
    }

    @Test 
    void birthDateWithSlashesViolation() {
        this.dto.setBirthDate("1990/05/15");
        assertHasViolations("birthDate");
    }

    @Test 
    void birthDateWithoutLeadingZeroViolation() {
        this.dto.setBirthDate("1990-5-15"); // Missing leading zero on month
        assertHasViolations("birthDate");
    }

    @Test 
    void birthDateWithoutZeroPaddedDayViolation() {
        this.dto.setBirthDate("1990-05-5"); // Missing leading zero on day
        assertHasViolations("birthDate");
    }

    @Test
    void nullHoldingListViolation() {
        this.dto.setHoldings(null);
        assertHasViolations("holdings");
    }
  
}
