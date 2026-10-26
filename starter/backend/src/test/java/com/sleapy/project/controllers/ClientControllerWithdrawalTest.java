package com.sleapy.project.controllers;

import com.sleapy.project.exceptions.InsufficientCashException;
import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerWithdrawalTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ClientService clientService;
    @MockitoBean private JwtService jwtService;

    @Test
    void successfulWithdrawalReturnsUpdatedBalance() throws Exception {
        when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
        when(clientService.withdrawCash(1L, new BigDecimal("25.50"))).thenReturn(100.0);
        mvc.perform(post("/api/clients/1/withdraw").header("Authorization", "test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":25.50}"))
                .andExpect(status().isOk()).andExpect(content().string("100.0"));
    }

    @Test
    void unauthenticatedWithdrawalIsRejected() throws Exception {
        mvc.perform(post("/api/clients/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(clientService);
    }

    @Test
    void businessErrorsUseExpectedStatusCodes() throws Exception {
        when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
        when(clientService.withdrawCash(1L, BigDecimal.ONE))
                .thenThrow(new IllegalStateException("Your account is frozen."))
                .thenThrow(new InsufficientCashException("Insufficient cash balance."))
                .thenThrow(new IllegalArgumentException("Invalid amount"))
                .thenThrow(new NoSuchElementException("Client not found"));
        for (int statusCode : new int[]{403, 422, 400, 404}) {
            mvc.perform(post("/api/clients/1/withdraw").header("Authorization", "test")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                    .andExpect(status().is(statusCode)).andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    void rejectsLettersInWithdrawalAmount() throws Exception {
        when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
        mvc.perform(post("/api/clients/1/withdraw").header("Authorization", "test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clientService);
    }
}
