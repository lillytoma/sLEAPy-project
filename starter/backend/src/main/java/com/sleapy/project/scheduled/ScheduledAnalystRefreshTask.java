package com.sleapy.project.scheduled;

import com.sleapy.project.websocket.AnalystWebSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "spring.task.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ScheduledAnalystRefreshTask {
    private final AnalystWebSocketController analystWebSocketController;

    @Scheduled(fixedDelayString = "${analyst.refresh-ms:10000}")
    public void refreshAnalystSummary() {
        try {
            analystWebSocketController.broadcastSummary();
        } catch (RuntimeException exception) {
            log.error("Error refreshing analyst summary: {}", exception.getMessage(), exception);
        }
    }
}
