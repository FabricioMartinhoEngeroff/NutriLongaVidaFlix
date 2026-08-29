package com.dvFabricio.VidaLongaFlix.domain.shared;

public final class StringValidator {

    private StringValidator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
