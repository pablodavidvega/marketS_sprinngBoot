package com.example.MarketS.service;

import com.example.MarketS.model.ConfiguracionApp;
import com.example.MarketS.repository.ConfiguracionAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguracionAppService {

    public static final String MODO_REGISTRO    = "modo_registro";
    public static final String DOMINIO_EMAIL    = "dominio_email";

    // Valores posibles de modo_registro
    public static final String MODO_ABIERTO             = "ABIERTO";
    public static final String MODO_DOMINIO             = "DOMINIO";
    public static final String MODO_INVITACION          = "INVITACION";
    public static final String MODO_DOMINIO_O_INVITACION = "DOMINIO_O_INVITACION";

    private final ConfiguracionAppRepository repo;

    public String get(String clave, String valorDefecto) {
        return repo.findById(clave)
                .map(ConfiguracionApp::getValor)
                .orElse(valorDefecto);
    }

    public void set(String clave, String valor) {
        repo.save(new ConfiguracionApp(clave, valor));
    }

    public void delete(String clave) {
        repo.deleteById(clave);
    }

    public String getModoRegistro() {
        return get(MODO_REGISTRO, MODO_ABIERTO);
    }

    public String getDominioPermitido() {
        return get(DOMINIO_EMAIL, "");
    }
}
