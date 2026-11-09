package ru.spb.transit.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.spb.transit.service.ReliabilityService;
import ru.spb.transit.service.TripService;
import ru.spb.transit.service.TripStatusService;
import ru.spb.transit.web.dto.CreateTripRequest;
import ru.spb.transit.web.dto.ReliabilityResponse;
import ru.spb.transit.web.dto.TripListResponse;
import ru.spb.transit.web.dto.TripResponse;
import ru.spb.transit.web.dto.TripStatusResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final TripStatusService tripStatusService;
    private final ReliabilityService reliabilityService;

    public TripController(TripService tripService,
                          TripStatusService tripStatusService,
                          ReliabilityService reliabilityService) {
        this.tripService = tripService;
        this.tripStatusService = tripStatusService;
        this.reliabilityService = reliabilityService;
    }

    @GetMapping
    public TripListResponse findAll(Authentication authentication) {
        return new TripListResponse(tripService.findAllForUser(authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody CreateTripRequest request,
                               Authentication authentication) {
        return tripService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}/status")
    public TripStatusResponse status(@PathVariable Long id, Authentication authentication) {
        return tripStatusService.getStatus(authentication.getName(), id);
    }

    @GetMapping("/{id}/reliability")
    public ReliabilityResponse reliability(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int periodDays,
            Authentication authentication) {
        return reliabilityService.getReliability(authentication.getName(), id, periodDays);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        tripService.delete(authentication.getName(), id);
    }
}
