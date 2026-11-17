package ru.spb.transit.integration.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.aggregator",
        name = "enabled",
        havingValue = "true"
)
public class AggregatorStartup {
    private static final Logger log = LoggerFactory.getLogger(AggregatorStartup.class);

    private final ApiScheduler scheduler;

    @Value("${app.aggregator.max-tasks:2}")
    private int maxTasks;

    @Value("${app.aggregator.interval-seconds:10}")
    private int intervalSeconds;

    public AggregatorStartup(ApiScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startAggregator() {
        log.info("Запуск агрегатора...");
        scheduler.start(maxTasks, intervalSeconds);
    }
}
