package com.sleapy.project.scheduled;

import com.sleapy.project.services.AnalystPortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledPortfolioSnapshotTask {
    private final AnalystPortfolioService service;

    @Scheduled(fixedDelay = 300000)
    public void capture() {
        service.capture();
    }
}
