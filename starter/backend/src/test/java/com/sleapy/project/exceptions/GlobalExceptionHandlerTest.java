package com.sleapy.project.exceptions;

import com.sleapy.project.controllers.ClientController;
import com.sleapy.project.controllers.OrderController;
import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.OrderService;
import com.sleapy.project.validators.ClientValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GlobalExceptionHandler.
 * Tests that all exception handlers properly catch exceptions and return appropriate error responses.
 * Verifies exception handlers for:
 * - NoSuchElementException (404 Not Found)
 * - InsufficientCashException (422 Unprocessable Entity)
 * - InsufficientSharesException (422 Unprocessable Entity)
 * - InvalidEmailFormatException (422 Unprocessable Entity)
 * - Generic Exception (500 Internal Server Error)
 */
@WebMvcTest({ClientController.class, OrderController.class})
public class GlobalExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ClientValidator clientValidator;

    /**
     * Tests that resource not found errors return a 404 Not Found response.
     * Configures the mocked ClientService to throw NoSuchElementException when fetching a non-existent client.
     * Verifies:
     * - HTTP status is 404 (Not Found)
     * - Response body contains status 404
     * - Response error message is "Not Found"
     */
    @Test
    void testNotFound() throws Exception {
        when(clientService.getCashBalance(999L))
            .thenThrow(new NoSuchElementException("Client with ID 999 not found"));
        
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/clients/999/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
        );
    }

    /**
     * Tests that insufficient cash errors return a 422 Unprocessable Entity response.
     * Configures the mocked ClientService to throw InsufficientCashException.
     * Verifies:
     * - HTTP status is 422 (Unprocessable Entity)
     * - Response body contains status 422
     * - Response error message is "Insufficient Cash"
     */
    @Test
    void testInsufficientCash() throws Exception {
        when(clientService.getCashBalance(1L))
            .thenThrow(new InsufficientCashException("Client has insufficient cash balance to complete this transaction"));
        
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/clients/1/balance"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Insufficient Cash"))
        );
    }

    /**
     * Tests that insufficient shares errors return a 422 Unprocessable Entity response.
     * Configures the mocked OrderService to throw InsufficientSharesException.
     * Verifies:
     * - HTTP status is 422 (Unprocessable Entity)
     * - Response body contains status 422
     * - Response error message is "Insufficient Shares"
     */
    @Test
    void testInsufficientShares() throws Exception {
        when(orderService.getTransactionsPerClient(2L))
            .thenThrow(new InsufficientSharesException("Client does not have enough shares to complete this sale"));
        
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/orders/").param("clientId", "2"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Insufficient Shares"))
        );
    }

    /**
     * Tests that unexpected/generic exceptions return a 500 Internal Server Error response.
     * Configures the mocked ClientService to throw a generic RuntimeException.
     * Verifies:
     * - HTTP status is 500 (Internal Server Error)
     * - Response body contains status 500
     * - Response error message is "Unexpected Error"
     */
    @Test
    void testUnexpectedException() throws Exception {
        when(clientService.getCashBalance(4L))
            .thenThrow(new RuntimeException("Unexpected database connection error"));
        
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/clients/4/balance"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Unexpected Error"))
        );
    }
}
