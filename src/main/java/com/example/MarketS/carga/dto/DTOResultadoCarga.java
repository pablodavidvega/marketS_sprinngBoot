package com.example.MarketS.carga.dto;

import java.util.List;

public class DTOResultadoCarga {

    private int total;
    private int correctos;
    private int fallidos;
    private List<String> errores;

    public DTOResultadoCarga(int total, int correctos, int fallidos, List<String> errores) {
        this.total = total;
        this.correctos = correctos;
        this.fallidos = fallidos;
        this.errores = errores;
    }

    public int getTotal() {
        return total;
    }

    public int getCorrectos() {
        return correctos;
    }

    public int getFallidos() {
        return fallidos;
    }

    public List<String> getErrores() {
        return errores;
    }

    // Nuevo método para mantener compatibilidad
    public int getProcesados() {
        return correctos;
    }
}
