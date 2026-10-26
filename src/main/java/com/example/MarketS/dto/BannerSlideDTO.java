package com.example.MarketS.dto;

import com.example.MarketS.model.BannerSlide;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class BannerSlideDTO {

    private Long id;
    private String imagen;
    private String imagenMobile;
    private String texto;
    private String enlace;
    private int orden;
    private boolean activo;
    private Date fechaCreacion;

    public static BannerSlideDTO from(BannerSlide b) {
        return BannerSlideDTO.builder()
                .id(b.getId())
                .imagen(b.getImagen())
                .imagenMobile(b.getImagenMobile())
                .texto(b.getTexto())
                .enlace(b.getEnlace())
                .orden(b.getOrden())
                .activo(b.isActivo())
                .fechaCreacion(b.getFechaCreacion())
                .build();
    }
}
