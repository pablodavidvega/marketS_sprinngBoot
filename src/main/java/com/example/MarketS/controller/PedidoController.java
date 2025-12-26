package com.example.MarketS.controller;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.repository.PedidoRepository;
import java.util.Map;
import java.util.HashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/pedido")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;

    public PedidoController(PedidoService pedidoService,
                            PedidoRepository pedidoRepository,
                            UserRepository userRepository){
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/crear")
    public String crearPedido(HttpSession session, Model model) {
        System.out.println("=== DEBUG POST: /pedido/crear llamado ===");
        return procesarCrearPedido(session, model);
    }

    @GetMapping("/crear")
    public String mostrarFormulario() {
        return "redirect:/carrito"; // O muestra una página de confirmación
    }

    // Lógica común para crear pedido
    private String procesarCrearPedido(HttpSession session, Model model) {
        System.out.println("\n=== 🎯 INICIO CREAR PEDIDO ===");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("1. Auth: " + (auth != null ? auth.getName() : "null"));

        if (auth == null) {
            System.out.println("❌ ERROR: Auth null, redirigiendo a login");
            return "redirect:/login";
        }

        String principal = auth.getName();
        User usuario = userRepository.findByEmail(principal)
                .orElseGet(() -> userRepository.findByUsername(principal).orElse(null));

        System.out.println("2. Usuario encontrado: " + (usuario != null ? usuario.getUsername() : "null"));

        if (usuario == null) {
            System.out.println("❌ ERROR: Usuario no encontrado en BD");
            return "redirect:/login";
        }

        Object carritoObj = session.getAttribute("carrito");
        System.out.println("3. Carrito en sesión (raw): " + carritoObj);

        Map<Integer, Integer> carrito = new HashMap<>();

        if (carritoObj instanceof Map<?, ?>) {
            Map<?, ?> raw = (Map<?, ?>) carritoObj;
            System.out.println("4. Carrito raw tamaño: " + raw.size());

            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                try {
                    Integer productId;
                    Integer quantity;

                    Object keyObj = entry.getKey();
                    Object valueObj = entry.getValue();

                    // Obtener productId de la clave
                    if (keyObj instanceof Integer) {
                        productId = (Integer) keyObj;
                    } else {
                        productId = Integer.valueOf(keyObj.toString());
                    }

                    // Manejar objeto complejo
                    if (valueObj instanceof Map) {
                        // El valor es un Map con propiedades del producto
                        Map<?, ?> productoMap = (Map<?, ?>) valueObj;
                        Object cantidadObj = productoMap.get("cantidad");

                        if (cantidadObj instanceof Integer) {
                            quantity = (Integer) cantidadObj;
                        } else {
                            quantity = Integer.valueOf(cantidadObj.toString());
                        }

                        System.out.println("   ✅ Producto ID: " + productId + ", Cantidad: " + quantity + " (de objeto complejo)");

                    } else if (valueObj instanceof Integer) {
                        // El valor es directamente la cantidad (formato antiguo)
                        quantity = (Integer) valueObj;
                        System.out.println("   ✅ Producto ID: " + productId + ", Cantidad: " + quantity + " (formato simple)");

                    } else {
                        // Intentar convertir de string
                        quantity = Integer.valueOf(valueObj.toString());
                        System.out.println("   ✅ Producto ID: " + productId + ", Cantidad: " + quantity + " (convertido de string)");
                    }

                    carrito.put(productId, quantity);

                } catch (NumberFormatException e) {
                    System.out.println("   ⚠️ Item inválido: " + entry.getKey() + "=" + entry.getValue());
                } catch (Exception e) {
                    System.out.println("   ❌ Error procesando item: " + e.getMessage());
                }
            }
        }

        System.out.println("5. Carrito procesado tamaño: " + carrito.size());

        if (carrito == null || carrito.isEmpty()) {
            System.out.println("❌ ERROR: Carrito vacío, redirigiendo a /carrito");
            return "redirect:/carrito";
        }

        System.out.println("6. Llamando a pedidoService.crearPedido()...");
        try {
            Pedido pedido = pedidoService.crearPedido(usuario, carrito);
            System.out.println("✅ PEDIDO CREADO EXITOSAMENTE");
            System.out.println("   - ID Pedido: " + pedido.getId());
            System.out.println("   - Estado: " + pedido.getEstado());
            System.out.println("   - Total: " + pedido.getTotal());
            System.out.println("   - Detalles: " + (pedido.getDetalles() != null ? pedido.getDetalles().size() : 0));

            model.addAttribute("pedido", pedido);
            System.out.println("7. Redirigiendo a pago.html");
            return "pago";

        } catch (Exception e) {
            System.out.println("❌ ERROR AL CREAR PEDIDO: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/carrito";
        }
    }


    @PostMapping("/pagar/{id}")
    public String pagarPedido(@PathVariable Long id,
                              HttpSession session,
                              Model model) {

        System.out.println("=== DEBUG: /pagar/" + id + " llamado ===");

        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        System.out.println("DEBUG: Pedido encontrado para pago - ID: " + pedido.getId());

        // cambiar estado
        pedido.setEstado("PAGADO");
        pedidoRepository.save(pedido);
        System.out.println("DEBUG: Estado cambiado a PAGADO");

        // vaciar carrito
        session.removeAttribute("carrito");
        System.out.println("DEBUG: Carrito eliminado de sesión");

        model.addAttribute("pedido", pedido);
        System.out.println("DEBUG: Redirigiendo a confirmacion.html");
        return "confirmacion";
    }
}