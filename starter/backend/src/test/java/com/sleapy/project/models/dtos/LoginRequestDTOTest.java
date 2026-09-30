package com.sleapy.project.models.dtos;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class LoginRequestDTOTest {

    @Autowired
    private Validator validator;

    private LoginRequestDTO dto;

    @BeforeEach
    void setUp() {
        dto = new LoginRequestDTO();
        dto.setEmail("user@example.com");
        dto.setPassword("SecurePassword123!");
    }

    void assertHasViolations(String violatedField) {
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(this.dto);
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(violatedField));
    }

    @Test
    void validLoginRequestHasNoViolations() {
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(this.dto);
        assertThat(violations).isEmpty();
    }

    // Email tests - @NotBlank + @Email
    @Test
    void nullEmailViolation() {
        this.dto.setEmail(null);
        assertHasViolations("email");
    }

    @Test
    void emptyEmailViolation() {
        this.dto.setEmail("");
        assertHasViolations("email");
    }

    @Test
    void blankEmailViolation() {
        this.dto.setEmail("   ");
        assertHasViolations("email");
    }

    @Test
    void emailWithoutAtSymbolViolation() {
        this.dto.setEmail("useremail.com");
        assertHasViolations("email");
    }

    @Test
    void emailWithoutDomainViolation() {
        this.dto.setEmail("user@");
        assertHasViolations("email");
    }

    @Test
    void emailWithoutLocalPartViolation() {
        this.dto.setEmail("@example.com");
        assertHasViolations("email");
    }

    @Test
    void emailWithSpacesViolation() {
        this.dto.setEmail("user @example.com");
        assertHasViolations("email");
    }

    @Test
    void emailWithMultipleAtSymbolsViolation() {
        this.dto.setEmail("user@@example.com");
        assertHasViolations("email");
    }

    @Test
    void validBasicEmailIsValid() {
        this.dto.setEmail("john.doe@example.com");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void validEmailWithPlusTagIsValid() {
        this.dto.setEmail("user+tag@example.co.uk");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void validEmailWithNumbersIsValid() {
        this.dto.setEmail("user123@example456.com");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    @Test
    void validEmailWithDashIsValid() {
        this.dto.setEmail("user-name@example.com");
        assertThat(validator.validate(this.dto)).isEmpty();
    }

    // Password tests - @NotBlank
    @Test
    void nullPasswordViolation() {
        this.dto.setPassword(null);
        assertHasViolations("password");
    }

    @Test
    void emptyPasswordViolation() {
        this.dto.setPassword("");
        assertHasViolations("password");
    }

    @Test
    void blankPasswordViolation() {
        this.dto.setPassword("   ");
        assertHasViolations("password");
    }

}
