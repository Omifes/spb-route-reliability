package ru.spb.transit.web.error;

public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(Long tripId) {
        super("Поездка с идентификатором " + tripId + " не найдена");
    }
}
