package ru.spb.transit.integration.aggregator.api;

import ru.spb.transit.integration.aggregator.model.ApiRecord;

public interface ApiClient {
    String getName();

    ApiRecord fetch() throws Exception;
}
