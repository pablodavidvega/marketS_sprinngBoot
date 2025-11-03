package com.example.MarketS.model;

import java.util.List;

public class Correo {
    private String asunto;
    private String mensaje;
    private List<String> destinatarios;

    private Correo() {} // constructor privado

    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public List<String> getDestinatarios() { return destinatarios; }

    //Builder interno
    public static class Builder {
        private String asunto;
        private String mensaje;
        private List<String> destinatarios;

        public Builder asunto(String asunto) {
            this.asunto = asunto;
            return this;
        }

        public Builder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }

        public Builder destinatarios(List<String> destinatarios) {
            this.destinatarios = destinatarios;
            return this;
        }

        public Correo build() {
            Correo correo = new Correo();
            correo.asunto = this.asunto;
            correo.mensaje = this.mensaje;
            correo.destinatarios = this.destinatarios;
            return correo;
        }
    }
}
