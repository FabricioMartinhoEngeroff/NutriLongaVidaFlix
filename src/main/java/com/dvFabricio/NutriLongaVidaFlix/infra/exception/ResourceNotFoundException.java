package com.dvFabricio.NutriLongaVidaFlix.infra.exception;

public class ResourceNotFoundException  extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
