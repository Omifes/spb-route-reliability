package ru.spb.transit.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record TripStatusResponse(
        Long tripId,
        String route,
        String nextStop,
        Integer etaMinutes,
        @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime,
        @JsonFormat(pattern = "HH:mm") LocalTime requiredArrivalTime,
        Integer delayMinutes,
        Boolean willArriveOnTime,
        DataStatus dataStatus,
        LocalDateTime dataUpdatedAt
) {

    public enum DataStatus {
        CURRENT("current"),
        STALE("stale"),
        UNAVAILABLE("unavailable");

        private final String value;

        DataStatus(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }
}
