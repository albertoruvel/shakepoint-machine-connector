package com.shakepoint.web.io.email;

public enum Email {

    PRODUCT_LOW_LEVEL_ALERT("product_low_levels","Producto bajo en maquina"),
    PRODUCT_LOW_LEVEL_CRITICAL("product_insufficient","Producto menos de 15% en maquina"),
    MACHINE_FAILED("machine_failure","Ha ocurrido un error en una maquina"),
    MACHINE_RECEIVED_NO_VALID_MESSAGE("unknown_machine_code","Se ha recibido un mensaje no identificado en una maquina");

    Email(final String templateName, final String subject){
        this.subject = subject;
        this.templateName = templateName;
    }

    private String subject;
    private String templateName;

    public String getSubject() {
        return subject;
    }

    public String getTemplateName() {
        return templateName;
    }
}
