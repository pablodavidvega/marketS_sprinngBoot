package com.example.MarketS.controller;

import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.service.ContactoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/servicio-cliente")
public class AdminContactoController {

    @Autowired
    private ContactoService contactoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("solicitudes", contactoService.listarTodos());
        model.addAttribute("totalPendientes", contactoService.contarPorEstado(EstadoContacto.PENDIENTE));
        model.addAttribute("totalEnProceso", contactoService.contarPorEstado(EstadoContacto.EN_PROCESO));
        model.addAttribute("totalResueltos", contactoService.contarPorEstado(EstadoContacto.RESUELTO));
        model.addAttribute("activePage", "servicioCliente");
        return "admin/servicio-cliente";
    }

    @PostMapping("/{id}/responder")
    public String responder(@PathVariable Long id, @RequestParam String respuesta,
                            RedirectAttributes redirectAttributes) {
        contactoService.responder(id, respuesta);
        redirectAttributes.addFlashAttribute("mensajeExito", "Respuesta enviada correctamente.");
        return "redirect:/admin/servicio-cliente";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoContacto estado,
                                RedirectAttributes redirectAttributes) {
        contactoService.cambiarEstado(id, estado);
        redirectAttributes.addFlashAttribute("mensajeExito", "Estado actualizado correctamente.");
        return "redirect:/admin/servicio-cliente";
    }
}
