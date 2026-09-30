package com.sleapy.project.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GlobalExceptionHandler.
 * Tests that all exception handlers properly catch exceptions and return appropriate error responses.
 */
@WebMvcTest(GlobalExceptionHandler.class) // Test only the web layer (controller)
public class GlobalExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests and captures responses
    
    /**
     * Tests that validation errors return a 400 Bad Request response when required fields are missing.
     * Sends a POST request with an empty email field and verifies:
     * - HTTP status is 400 (Bad Request)
     * - Response body contains status 400
     * - Response error message is "Bad Request"
     */
    @Test
    void testValidationError() throws Exception {
        assertDoesNotThrow(() ->
            mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"\"}")) // Invalid - empty email
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
        );
    }
    
    /**
     * Tests that resource not found errors return a 404 Not Found response.
     * Sends a GET request for a non-existent client (ID 999) and verifies:
     * - HTTP status is 404 (Not Found)
     * - Response body contains status 404
     * - Response error message is "Not Found"
     */
    @Test
    void testNotFound() throws Exception {
        assertDoesNotThrow(() ->
            mockMvc.perform(get("/api/clients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
        );
    }
    
    /**
     * Tests that insufficient cash errors return a 422 Unprocessable Entity response.
     * Sends a POST request attempting to withdraw more money than the client's current balance and verifies:
     * - HTTP status is 422 (Unprocessable Entity) - business logic error
     * - Response body contains status 422
     * - Response error message is "Insufficient Cash"
     */
    @Test
    void testInsufficientCash() throws Exception {
        assertDoesNotThrow(() ->
            mockMvc.perform(post("/api/orders/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 10000}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Insufficient Cash"))
        );
    }
}