package com.sleapy.project.websocket;

import com.sleapy.project.models.dtos.AnalystSummaryDTO;
import com.sleapy.project.services.AnalystService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AnalystWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final AnalystService analystService;

    public void broadcastSummary() {
        AnalystSummaryDTO summary = analystService.getSummary();
        messagingTemplate.convertAndSend("/topic/analyst", summary);
    }
}
