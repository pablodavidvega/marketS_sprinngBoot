package com.example.MarketS.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "_hallazgo")
public class Hallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prueba;
    private String tipo;
    private String resultado;
    private String descripcion;
    private LocalDateTime fecha;

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrueba() { return prueba; }
    public void setPrueba(String prueba) { this.prueba = prueba; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
