package com.sleapy.project.models.dtos;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Data transfer object for client information.
 * Used for API requests/responses.
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class ClientDTO {

    @NotNull(message = "client id is required")
    @Positive(message = "client id must be positive")
    private Long id; // Unique client identifier

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email; // Email address (login credential)

    @NotBlank(message = "username is required")
    private String username; // Display username

    @NotNull(message = "cash balance cannot be null")
    @PositiveOrZero(message = "cash balance must be zero or positive")
    private double cashBalance; // Available cash balance

    @Pattern(regexp = "^[0-9]{10}$", message = "phone number must be 10 digits")
    private String phoneNumber; // Contact phone number

    @NotBlank(message = "SSN last 4 digits are required")
    @Pattern(regexp = "^[0-9]{4}$", message = "SSN last 4 must be 4 digits")
    private String ssnLast4; // Last 4 digits of SSN

    @NotBlank(message = "full address is required")
    private String fullAddress; // Residential address

    @NotBlank(message = "birth date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birth date must be in YYYY-MM-DD format")
    private String birthDate; // Birth date (YYYY-MM-DD format)

    @Valid
    private ArrayList<HoldingDTO> holdings; // List of all holdings owned
}
