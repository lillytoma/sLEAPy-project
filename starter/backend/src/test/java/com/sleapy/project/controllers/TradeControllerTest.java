package com.sleapy.project.controllers;

import com.sleapy.project.models.dtos.TradeResponseDTO;
import com.sleapy.project.services.JwtService;
import com.sleapy.project.services.OrderStatusNotificationService;
import com.sleapy.project.services.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
class TradeControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private TradeService tradeService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private OrderStatusNotificationService notifications;

    @BeforeEach
    void authorize() {
        lenient().when(jwtService.verifyAuthorizationHeader(anyString())).thenReturn(true);
    }

    @Test
    void returnsQueuedStatusWithoutExecutionAmounts() throws Exception {
        TradeResponseDTO response = new TradeResponseDTO();
        response.setStatus("QUEUED");
        response.setOrderId(10L);
        response.setMessage("Queued for next regular session");
        when(tradeService.buyStock(any())).thenReturn(response);
        mvc.perform(post("/api/trades/buy").header("Authorization", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"clientId":1,"instrumentId":2,"quantity":1,"orderType":"MARKET","session":"REGULAR"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.totalAmount").doesNotExist());
    }

    @Test
    void rejectsUnsupportedOrderTypesForBothSides() throws Exception {
        for (String side : new String[]{"buy", "sell"}) {
            mvc.perform(post("/api/trades/" + side).header("Authorization", "test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"clientId":1,"instrumentId":2,"quantity":1,"orderType":"LIMIT"}
                        """))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(tradeService);
    }

    @Test
    void rejectsExtendedHoursExecutionForBothSides() throws Exception {
        for (String side : new String[]{"buy", "sell"}) {
            mvc.perform(post("/api/trades/" + side).header("Authorization", "test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"clientId":1,"instrumentId":2,"quantity":1,"session":"EXTENDED"}
                        """))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(tradeService);
    }
}
