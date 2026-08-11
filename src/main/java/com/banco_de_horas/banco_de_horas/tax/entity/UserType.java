package com.banco_de_horas.banco_de_horas.tax.entity;

public enum UserType {
    FISCAL("Fiscal"),
    SUPERVISOR("Supervisor"),
    ADMINISTRADOR_SUPERVISOR("Administrador Supervisor"),
    ADMINISTRADOR_FISCAL("Administrador Fiscal");

    private String userType;

    UserType(String userType) {
        this.userType = userType;
    }

    public String getUserType() {
        return this.userType;
    }
}
