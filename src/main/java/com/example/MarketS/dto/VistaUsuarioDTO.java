package com.example.MarketS.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VistaUsuarioDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String rol;
    private String city; //Solo vendedores//

}
