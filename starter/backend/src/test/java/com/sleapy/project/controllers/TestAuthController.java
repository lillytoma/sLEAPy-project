package com.sleapy.project.controllers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sleapy.project.exceptions.InvalidEmailFormatException;
import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.models.dtos.LoginRequestDTO;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.JwtService;
import com.sleapy.project.validators.ClientValidator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@WebMvcTest(AuthController.class) // Test only the web layer (controller)
class TestAuthController {
 
    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests
 
    @MockitoBean
    private ClientService clientService;
 
    @MockitoBean
    private ClientValidator clientValidator;

    @MockitoBean
    private ClientMapper clientMapper;

    @MockitoBean
    private JwtService jwtService;
 
    // Test cases will go here
    @Test
    void loginReturnsOKForValidCredentials() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(
            "Test@example.com",
            "TestPassword123"
        );

        ClientEntity user = new ClientEntity();
        user.setId(1L);
        user.setEmail(request.getEmail());

        when(clientService.checkCredentials(anyString(), anyString())).thenReturn(true);
        when(clientMapper.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(request.getEmail(), 1L)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + request.getEmail() + "\",\"password\":\"" + request.getPassword() + "\"}"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("Login successful"))
            .andExpect(jsonPath("$.userID").value(1))
            .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        //Arrange
        LoginRequestDTO request = new LoginRequestDTO(
            "Test@example.com",
            "WrongPassword123"
        );
        when(clientService.checkCredentials(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + request.getEmail() + "\", \"password\":\"" + request.getPassword() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    
    }
    
    @Test
    void loginReturnsBadRequestForInvalidEmail() throws Exception {
        //Arrange
        LoginRequestDTO request = new LoginRequestDTO(
            "This isn't even an email",
            "WrongPassword123"
        );
        
        doThrow(new InvalidEmailFormatException("Invalid email format"))
            .when(clientValidator)
            .validateEmail(anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + request.getEmail() + "\", \"password\":\"" + request.getPassword() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}