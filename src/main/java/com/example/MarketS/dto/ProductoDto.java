package com.example.MarketS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductoDto {

    private int id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre no puede exceder 100 caracteres.")
    private String nombre;

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser positivo.")
    private Double precio;

    @NotNull(message = "La fecha de agregación es obligatoria.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaAgregacion;

    @NotBlank(message = "Debe seleccionar el estado del producto.")
    private String estadoProducto;

    @Size(max = 300, message = "La descripción no puede exceder 300 caracteres.")
    private String descripcion;

    private MultipartFile imagen;

    @NotBlank(message = "El nombre de la marca es obligatorio.")
    private String nombreMarca;

    @NotNull(message = "Debe seleccionar una categoría.")
    private Long categoriaId;

    private List<TallasStockDTO> tallas = new ArrayList<>();

    @NotNull(message = "El stock es obligatorio.")
    @Positive(message = "El stock debe ser mayor a 0.")
    private Integer stock;   // 👈 este es el stock general

    public Integer getStock() {return stock;}

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public List<TallasStockDTO> getTallas() {
        return tallas;
    }

    public void setTallas(List<TallasStockDTO> tallas) {
        this.tallas = tallas;
    }

    private List<MultipartFile> imagenes;
    private MultipartFile nuevaImagen; // no "imagen", para no chocar con la String

    public MultipartFile getNuevaImagen() {
        return nuevaImagen;
    }

    public void setNuevaImagen(MultipartFile nuevaImagen) {
        this.nuevaImagen = nuevaImagen;
    }

    public List<MultipartFile> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<MultipartFile> imagenes) {
        this.imagenes = imagenes;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Date getFechaAgregacion() {
        return fechaAgregacion;
    }

    public void setFechaAgregacion(Date fechaAgregacion) {
        this.fechaAgregacion = fechaAgregacion;
    }

    public String getEstadoProducto() {
        return estadoProducto;
    }

    public void setEstadoProducto(String estadoProducto) {
        this.estadoProducto = estadoProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public MultipartFile getImagen() {
        return imagen;
    }

    public void setImagen(MultipartFile imagen) {
        this.imagen = imagen;
    }

    public String getNombreMarca() {
        return nombreMarca;
    }

    public void setNombreMarca(String nombreMarca) {
        this.nombreMarca = nombreMarca;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}

