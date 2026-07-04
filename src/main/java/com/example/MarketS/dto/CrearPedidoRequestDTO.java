package com.example.MarketS.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CrearPedidoRequestDTO {

    @NotEmpty(message = "El carrito no puede estar vacío")
    @Valid
    private List<ItemCarritoDTO> items;

    // MERCADOPAGO (default si viene vacío), o un acuerdo externo: EFECTIVO, TRANSFERENCIA, NEQUI, OTRO
    private String metodoPago;

    @Getter
    @Setter
    public static class ItemCarritoDTO {
        @NotNull
        private Long productoId;

        @NotNull
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
    }
}
