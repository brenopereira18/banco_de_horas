package com.banco_de_horas.banco_de_horas.exceptions;

public class EntityAlreadyExists extends RuntimeException {
    public EntityAlreadyExists(String message) {
        super(message);
    }
}
