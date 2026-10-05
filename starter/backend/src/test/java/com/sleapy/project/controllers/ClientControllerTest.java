package com.sleapy.project.controllers;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.NoSuchElementException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.closeTo;

import com.sleapy.project.services.ClientService;
import com.sleapy.project.validators.ClientValidator;

@WebMvcTest(ClientController.class) // Test only the web layer (controller)
public class ClientControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ClientService clientService;

    @MockitoBean 
    private ClientValidator clientValidator;


    @Test
    void getCashBalanceReturnsOKForValidID() {

        // Test implementation will go here
        when(clientService.getCashBalance(1L)).thenReturn(100.0);

        // Act & Assert
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/clients/1/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(closeTo(100.0, 0.01)))
        );
    }

    @Test
    void getCashBalanceReturnsNotFoundForInvalidID() throws Exception {

        // Arrange - Mock service to throw exception
        when(clientService.getCashBalance(999L)).thenThrow(new NoSuchElementException("Client not found"));

        // Act & Assert - Verify HTTP 404 response
        mockMvc.perform(get("/api/clients/999/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Client not found"));
    }

}
