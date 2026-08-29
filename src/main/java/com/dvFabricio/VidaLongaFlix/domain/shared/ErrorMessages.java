package com.dvFabricio.VidaLongaFlix.domain.shared;

import java.util.UUID;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static String userNotFound(UUID id) {
        return "User not found with id: " + id;
    }

    public static String videoNotFound(UUID id) {
        return "Video with ID " + id + " not found.";
    }

    public static String categoryNotFound(UUID id) {
        return "Category with ID " + id + " not found.";
    }

    public static String commentNotFound(UUID id) {
        return "Comment with ID " + id + " not found.";
    }

    public static String menuNotFound(UUID id) {
        return "Menu with ID " + id + " not found.";
    }
}
