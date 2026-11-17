package ru.spb.transit.integration.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.integration.aggregator.api.ApiClient;
import ru.spb.transit.integration.aggregator.model.ApiRecord;
import ru.spb.transit.repository.TransportUpdateRepository;

import java.util.List;

public class ApiTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ApiTask.class);

    private final ApiClient client;
    private final AggregationService aggregationService;
    private final TransportUpdateRepository repository;

    public ApiTask(
            ApiClient client,
            AggregationService aggregationService,
            TransportUpdateRepository repository
    ) {
        this.client = client;
        this.aggregationService = aggregationService;
        this.repository = repository;
    }

    @Override
    public void run() {
        try {
            log.info("Запрос: {}", client.getName());
            ApiRecord record = client.fetch();
            List<TransportUpdate> updates = aggregationService.aggregate(record);

            if (updates.isEmpty()) {
                log.info("Источник {} не вернул транспортных обновлений", client.getName());
                return;
            }

            repository.saveAll(updates);
            log.info("Сохранено {} обновлений от источника {}",
                    updates.size(), client.getName());
        } catch (Exception exception) {
            log.error("Ошибка при опросе источника {}", client.getName(), exception);
        }
    }
}
