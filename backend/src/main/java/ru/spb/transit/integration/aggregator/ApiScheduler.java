package ru.spb.transit.integration.aggregator;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spb.transit.integration.aggregator.api.ApiClient;
import ru.spb.transit.repository.TransportUpdateRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ApiScheduler {
    private static final Logger log = LoggerFactory.getLogger(ApiScheduler.class);

    private final ApiRegistry registry;
    private final AggregationService aggregationService;
    private final TransportUpdateRepository repository;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private ScheduledExecutorService executor;

    public ApiScheduler(
            ApiRegistry registry,
            AggregationService aggregationService,
            TransportUpdateRepository repository
    ) {
        this.registry = registry;
        this.aggregationService = aggregationService;
        this.repository = repository;
    }

    public synchronized void start(int maxTasks, int intervalSeconds) {
        if (!started.compareAndSet(false, true)) {
            log.warn("Агрегатор уже запущен");
            return;
        }
        if (maxTasks < 1 || intervalSeconds < 1) {
            started.set(false);
            throw new IllegalArgumentException("maxTasks and intervalSeconds must be positive");
        }

        executor = Executors.newScheduledThreadPool(maxTasks);
        for (ApiClient client : registry.getClients().values()) {
            ApiTask task = new ApiTask(client, aggregationService, repository);
            executor.scheduleWithFixedDelay(
                    task, 0, intervalSeconds, TimeUnit.SECONDS
            );
            log.info("Источник {} зарегистрирован: интервал {} сек.",
                    client.getName(), intervalSeconds);
        }
    }

    @PreDestroy
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        started.set(false);
        log.info("Планировщик агрегатора остановлен");
    }
}
