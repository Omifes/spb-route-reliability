package ru.spb.transit.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateTripRequest(
        @NotBlank(message = "Название поездки обязательно")
        @Size(max = 255, message = "Название поездки не должно превышать 255 символов")
        String name,

        @NotBlank(message = "Идентификатор маршрута обязателен")
        @Size(max = 100, message = "Идентификатор маршрута слишком длинный")
        String routeId,

        @NotBlank(message = "Идентификатор остановки отправления обязателен")
        @Size(max = 100, message = "Идентификатор остановки отправления слишком длинный")
        String fromStopId,

        @NotBlank(message = "Идентификатор остановки назначения обязателен")
        @Size(max = 100, message = "Идентификатор остановки назначения слишком длинный")
        String toStopId,

        @NotNull(message = "Требуемое время прибытия обязательно")
        @JsonFormat(pattern = "HH:mm")
        LocalTime requiredArrivalTime
) {
}
