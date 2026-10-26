package com.sleapy.project.controllers;

import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerDepositTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ClientService clientService;
    @MockitoBean private JwtService jwtService;

    @Test
    void frozenDepositReturnsForbiddenWithExplanation() throws Exception {
        when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
        when(clientService.addCash(1L, new BigDecimal("25.50")))
                .thenThrow(new IllegalStateException("Your account is frozen."));
        mvc.perform(post("/api/clients/1/deposit").header("Authorization", "test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":25.50}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Your account is frozen."));
    }

    @Test
    void rejectsLettersInDepositAmount() throws Exception {
        when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
        mvc.perform(post("/api/clients/1/deposit").header("Authorization", "test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clientService);
    }
}
