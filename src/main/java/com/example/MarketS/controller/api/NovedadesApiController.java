package com.example.MarketS.controller.api;

import com.example.MarketS.model.User;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.NovedadesDiariasService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Resumen diario "Novedades del día" (ver NovedadesDiariasService)
@RestController
@RequestMapping("/api")
public class NovedadesApiController {

    private final NovedadesDiariasService novedadesDiariasService;
    private final UserRepository userRepository;

    public NovedadesApiController(NovedadesDiariasService novedadesDiariasService, UserRepository userRepository) {
        this.novedadesDiariasService = novedadesDiariasService;
        this.userRepository = userRepository;
    }

    // POST /api/novedades/baja?token=... — público (permitAll en SecurityConfig): lo llaman la
    // página de confirmación del frontend y el botón "Anular suscripción" de Gmail/Outlook
    // (List-Unsubscribe-Post), ninguno de los dos con sesión iniciada. Idempotente.
    @PostMapping("/novedades/baja")
    public ResponseEntity<Map<String, String>> darseDeBaja(@RequestParam String token) {
        User user = userRepository.findByTokenBajaNovedades(token).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "El enlace no es válido o ya no existe"));
        }
        if (user.isRecibirNovedades()) {
            user.setRecibirNovedades(false);
            userRepository.save(user);
        }
        return ResponseEntity.ok(Map.of("mensaje", "Ya no vas a recibir el resumen de novedades"));
    }

    // POST /api/admin/novedades/enviar — dispara el resumen sin esperar a las 7 p. m. (pruebas,
    // o si el servidor estuvo caído a esa hora). Solo anuncia productos que aún no salieron en
    // ningún resumen, así que no duplica lo que ya se envió.
    @PostMapping("/admin/novedades/enviar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<NovedadesDiariasService.Resultado> enviarAhora() {
        return ResponseEntity.ok(novedadesDiariasService.enviarResumen());
    }
}
