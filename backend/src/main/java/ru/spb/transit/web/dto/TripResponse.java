package ru.spb.transit.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record TripResponse(
        Long id,
        String name,
        String routeId,
        String fromStopId,
        String toStopId,
        @JsonFormat(pattern = "HH:mm") LocalTime requiredArrivalTime
) {
}
