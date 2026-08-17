package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ContactoResponseDTO;
import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.model.Contacto;
import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.model.User;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.ContactoService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/contacto")
public class ContactoApiController {

    private final ContactoService contactoService;

    public ContactoApiController(ContactoService contactoService) {
        this.contactoService = contactoService;
    }

    // POST /api/contacto — enviar solicitud (público y autenticado)
    @PostMapping
    public ResponseEntity<ContactoResponseDTO> enviar(
            @RequestParam @NotBlank String nombre,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String tipoSolicitud,
            @RequestParam @NotBlank String asunto,
            @RequestParam @NotBlank @Size(max = 1000) String mensaje,
            Authentication auth) {

        Contacto contacto = new Contacto();
        contacto.setNombre(nombre.trim());
        contacto.setEmail(email.trim());
        contacto.setTipoSolicitud(tipoSolicitud);
        contacto.setAsunto(asunto.trim());
        contacto.setMensaje(mensaje.trim());
        contacto.setEstado(EstadoContacto.PENDIENTE);
        contacto.setFechaCreacion(LocalDateTime.now());

        User usuarioAutenticado = UsuarioPrincipal.from(auth);
        if (usuarioAutenticado != null) {
            contacto.setUser(usuarioAutenticado);
        }

        Contacto guardado = contactoService.guardar(contacto);
        return ResponseEntity.ok(ContactoResponseDTO.from(guardado));
    }

    // GET /api/contacto/mis-solicitudes — solicitudes del usuario autenticado, paginadas
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> misSolicitudes(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        User user = UsuarioPrincipal.from(auth);
        if (user == null) throw new RecursoNoEncontradoException("Usuario no encontrado");
        Page<Contacto> pagina = contactoService.listarPorUsuarioPaginado(user, PageRequest.of(page, size));
        List<ContactoResponseDTO> contenido = pagina.getContent().stream().map(ContactoResponseDTO::from).toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements()
        ));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    // GET /api/contacto/admin — todas las solicitudes, paginadas, con filtro opcional de
    // estado y estadísticas globales (no solo de la página actual).
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> listarTodas(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        EstadoContacto estadoFiltro = (estado == null || estado.isBlank()) ? null : EstadoContacto.valueOf(estado);
        Page<Contacto> pagina = contactoService.listarPaginado(estadoFiltro, PageRequest.of(page, size));
        List<ContactoResponseDTO> contenido = pagina.getContent().stream().map(ContactoResponseDTO::from).toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements(),
                "pendientes", contactoService.contarPorEstado(EstadoContacto.PENDIENTE),
                "enProceso",  contactoService.contarPorEstado(EstadoContacto.EN_PROCESO),
                "resueltos",  contactoService.contarPorEstado(EstadoContacto.RESUELTO)
        ));
    }

    // PATCH /api/contacto/admin/{id}/responder
    @PatchMapping("/admin/{id}/responder")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContactoResponseDTO> responder(
            @PathVariable Long id,
            @RequestParam @NotBlank String respuesta) {
        Contacto actualizado = contactoService.responder(id, respuesta.trim());
        return ResponseEntity.ok(ContactoResponseDTO.from(actualizado));
    }

    // PATCH /api/contacto/admin/{id}/estado
    @PatchMapping("/admin/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContactoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        EstadoContacto nuevoEstado = EstadoContacto.valueOf(estado);
        Contacto actualizado = contactoService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(ContactoResponseDTO.from(actualizado));
    }
}
