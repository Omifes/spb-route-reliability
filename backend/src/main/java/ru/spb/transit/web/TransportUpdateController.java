package ru.spb.transit.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.spb.transit.domain.TransportUpdate;
import ru.spb.transit.repository.TransportUpdateRepository;
import ru.spb.transit.web.dto.TransportUpdateResponse;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/transport/updates")
public class TransportUpdateController {

    private final TransportUpdateRepository transportUpdateRepository;

    public TransportUpdateController(
            TransportUpdateRepository transportUpdateRepository) {
        this.transportUpdateRepository = transportUpdateRepository;
    }

    @GetMapping
    public List<TransportUpdateResponse> findLatest(
            @RequestParam(defaultValue = "50")
            @Min(1) @Max(200) int limit) {

        return transportUpdateRepository.findAll(
                        PageRequest.of(
                                0,
                                limit,
                                Sort.by(Sort.Direction.DESC, "observedAt")
                        )
                )
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransportUpdateResponse toResponse(TransportUpdate update) {
        return new TransportUpdateResponse(
                update.getId(),
                update.getSource(),
                update.getRouteExternalId(),
                update.getStopExternalId(),
                update.getTripExternalId(),
                update.getVehicleExternalId(),
                update.getEventType(),
                update.getObservedAt(),
                update.getScheduledAt(),
                update.getActualAt(),
                update.getDelaySeconds(),
                update.getStatus(),
                update.getPayload(),
                update.getReceivedAt()
        );
    }
}