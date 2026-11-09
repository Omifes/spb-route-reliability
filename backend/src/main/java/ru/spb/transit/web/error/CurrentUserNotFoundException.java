package ru.spb.transit.web.error;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException() {
        super("Текущий пользователь не найден");
    }
}
