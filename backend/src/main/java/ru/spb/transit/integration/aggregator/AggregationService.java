package ru.spb.transit.integration.aggregator;

import org.springframework.stereotype.Service;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.integration.aggregator.model.ApiRecord;

import java.util.List;

@Service
public class AggregationService {
    private final TransportUpdateMapper mapper;

    public AggregationService(TransportUpdateMapper mapper) {
        this.mapper = mapper;
    }

    public List<TransportUpdate> aggregate(ApiRecord record) {
        return mapper.toEntities(record);
    }
}
