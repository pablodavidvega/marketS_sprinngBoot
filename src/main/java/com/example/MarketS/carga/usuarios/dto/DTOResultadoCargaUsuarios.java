package com.example.MarketS.carga.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DTOResultadoCargaUsuarios {
    private int procesados;  // Usuarios leídos correctamente del archivo
    private int exitosos;    // Usuarios guardados en BD exitosamente
    private int fallidos;    // Total de fallos (lectura + guardado)
    private List<String> errores = new ArrayList<>();

    // getters y setters para todos los campos
    public int getProcesados() { return procesados; }
    public void setProcesados(int procesados) { this.procesados = procesados; }

    public int getExitosos() { return exitosos; }
    public void setExitosos(int exitosos) { this.exitosos = exitosos; }

    public int getFallidos() { return fallidos; }
    public void setFallidos(int fallidos) { this.fallidos = fallidos; }

    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }
}