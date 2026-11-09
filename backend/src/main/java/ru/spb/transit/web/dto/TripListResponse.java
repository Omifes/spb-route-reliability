package ru.spb.transit.web.dto;

import java.util.List;

public record TripListResponse(List<TripResponse> trips) {
}
