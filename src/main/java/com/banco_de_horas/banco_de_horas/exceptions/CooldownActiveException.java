package com.banco_de_horas.banco_de_horas.exceptions;

public class CooldownActiveException extends RuntimeException {
    public CooldownActiveException() {
        super("Aguarde 5 minutos antes de solicitar um novo link de redefinição.");
    }
}
