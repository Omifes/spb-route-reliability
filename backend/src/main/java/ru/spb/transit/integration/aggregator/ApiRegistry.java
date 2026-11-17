package ru.spb.transit.integration.aggregator;

import org.springframework.stereotype.Component;
import ru.spb.transit.integration.aggregator.api.ApiClient;
import ru.spb.transit.integration.aggregator.api.MockTransportApiClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ApiRegistry {
    private final Map<String, ApiClient> clients = new LinkedHashMap<>();

    public ApiRegistry() {
        register(new MockTransportApiClient());
    }

    public void register(ApiClient client) {
        if (client == null || client.getName() == null || client.getName().isBlank()) {
            throw new IllegalArgumentException("API client and its name must not be null or blank");
        }
        clients.put(client.getName().trim().toLowerCase(Locale.ROOT), client);
    }

    public ApiClient getClient(String name) {
        if (name == null) {
            return null;
        }
        return clients.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public Map<String, ApiClient> getClients() {
        return Collections.unmodifiableMap(clients);
    }
}
