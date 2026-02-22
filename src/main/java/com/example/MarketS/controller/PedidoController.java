package com.example.MarketS.controller;

import com.example.MarketS.config.MercadoPagoConfig;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.service.MercadoPagoService;
import com.example.MarketS.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.repository.PedidoRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/pedido")
public class PedidoController {
    @Autowired
    private MercadoPagoService mercadoPagoService;
    @Autowired
    private MercadoPagoConfig mercadoPagoConfig;

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;

    public PedidoController(PedidoService pedidoService,
                            PedidoRepository pedidoRepository,
                            UserRepository userRepository,
                            MercadoPagoService mercadoPagoService,  // Agregar
                            MercadoPagoConfig mercadoPagoConfig) {  // Agregar
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.userRepository = userRepository;
        this.mercadoPagoService = mercadoPagoService;  // Agregar
        this.mercadoPagoConfig = mercadoPagoConfig;    // Agregar
    }

    // En PedidoController.java, modificar los métodos para que puedan responder tanto a redirecciones como a AJAX

    @PostMapping("/crear")
    public String crearPedido(HttpSession session, Model model,
                              RedirectAttributes redirectAttributes) {

        System.out.println("SESSION ID: " + session.getId());
        System.out.println("CARRITO SESSION: " + session.getAttribute("carrito"));
        System.out.println("\n=== 🚀 PEDIDO CONTROLLER: crearPedido() ===");

        try {
            System.out.println("1. Creando pedido...");
            String resultado = procesarCrearPedido(session, model);

            Pedido pedido = (Pedido) model.getAttribute("pedido");
            if (pedido == null) {
                System.err.println("❌ Pedido no creado");
                redirectAttributes.addFlashAttribute("error", "No se pudo crear el pedido");
                return "redirect:/carrito";
            }

            System.out.println("✅ Pedido creado: #" + pedido.getId() + ", Total: $" + pedido.getTotal());

            // FORZAR MERCADO PAGO - SIEMPRE intentarlo
            System.out.println("🔄 FORZANDO MERCADO PAGO...");
            try {
                Map<String, Object> resultadoMP = mercadoPagoService.crearPreferencia(
                        pedido.getId(),
                        pedido.getTotal(),
                        "Compra MarketS - Pedido #" + pedido.getId(),
                        pedido.getUsuario().getEmail()
                );

                System.out.println("Resultado MercadoPago: " + resultadoMP);

                if (Boolean.TRUE.equals(resultadoMP.get("success"))) {
                    String paymentUrl = (String) resultadoMP.get("paymentUrl");
                    String preferenceId = (String) resultadoMP.get("preferenceId");

                    redirectAttributes.addFlashAttribute("paymentUrl", paymentUrl);
                    redirectAttributes.addFlashAttribute("preferenceId", preferenceId);
                    redirectAttributes.addFlashAttribute("mercadoPagoSuccess", true);

                    System.out.println("✅ MercadoPago configurado - URL: " + paymentUrl);
                } else {
                    System.err.println("❌ Error MercadoPago: " + resultadoMP.get("error"));
                    // Aún así pasar el error para mostrar en la vista
                    redirectAttributes.addFlashAttribute("errorMercadoPago", resultadoMP.get("error"));
                    redirectAttributes.addFlashAttribute("errorMercadoPagoDetails", resultadoMP);
                }

            } catch (Exception e) {
                System.err.println("⚠️ Excepción en MercadoPago: " + e.getMessage());
                redirectAttributes.addFlashAttribute("errorMercadoPago", "Error técnico: " + e.getMessage());
            }

            // Pasar el pedido de todas formas
            redirectAttributes.addFlashAttribute("pedido", pedido);
            redirectAttributes.addFlashAttribute("pedidoId", pedido.getId());

            System.out.println("🎯 Redirigiendo a paso 2 con MercadoPago habilitado");
            return "redirect:/carrito?step=2&pedidoId=" + pedido.getId();

        } catch (Exception e) {
            System.err.println("❌ Error en crearPedido: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/carrito";
        }
    }

    @PostMapping("/iniciar-mercadopago/{pedidoId}")
    @ResponseBody
    public Map<String, Object> iniciarPagoMercadoPago(@PathVariable Long pedidoId) {
        Map<String, Object> respuesta = new HashMap<>();

        System.out.println("\n=== 🔄 iniciarPagoMercadoPago llamado ===");
        System.out.println("Pedido ID solicitado: " + pedidoId);

        try {
            // Buscar el pedido
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> {
                        System.err.println("❌ Pedido no encontrado: " + pedidoId);
                        return new RuntimeException("Pedido no encontrado: " + pedidoId);
                    });

            System.out.println("✅ Pedido encontrado: #" + pedido.getId());
            System.out.println("   - Usuario: " + pedido.getUsuario().getEmail());
            System.out.println("   - Total: $" + pedido.getTotal());

            // Crear preferencia en MercadoPago
            Map<String, Object> resultadoMP = mercadoPagoService.crearPreferencia(
                    pedido.getId(),
                    pedido.getTotal(),
                    "Compra MarketS - Pedido #" + pedido.getId(),
                    pedido.getUsuario().getEmail()
            );

            System.out.println("📊 Resultado MercadoPago: " + resultadoMP);

            if (Boolean.TRUE.equals(resultadoMP.get("success"))) {
                respuesta.put("success", true);
                respuesta.put("paymentUrl", resultadoMP.get("paymentUrl"));
                respuesta.put("preferenceId", resultadoMP.get("preferenceId"));

                // Actualizar estado del pedido
                pedido.setEstado("PROCESANDO_PAGO");
                pedidoRepository.save(pedido);

                System.out.println("✅ Pago MercadoPago iniciado exitosamente");
            } else {
                respuesta.put("success", false);
                respuesta.put("error", resultadoMP.get("error"));
                System.err.println("❌ Error MercadoPago: " + resultadoMP.get("error"));
            }

        } catch (Exception e) {
            System.err.println("❌ Error en iniciarPagoMercadoPago: " + e.getMessage());
            e.printStackTrace();
            respuesta.put("success", false);
            respuesta.put("error", "Error al iniciar pago: " + e.getMessage());
        }

        System.out.println("📤 Respuesta enviada: " + respuesta);
        return respuesta;
    }

    @PostMapping("/pagar/{id}")
    public String pagarPedido(@PathVariable Long id,
                              HttpSession session,
                              Model model,
                              @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                              @RequestParam(required = false) String origen) {

        System.out.println("=== DEBUG: /pagar/" + id + " llamado ===");
        System.out.println("Origen: " + (origen != null ? origen : "directo"));

        try {
            Pedido pedido = pedidoRepository.findById(id).orElseThrow();
            System.out.println("DEBUG: Pedido encontrado para pago - ID: " + pedido.getId());

            // Solo marcar como PAGADO si viene de MercadoPago o es pago directo
            if ("mercadopago".equals(origen) || "directo".equals(origen)) {
                pedido.setEstado("PAGADO");
                pedidoRepository.save(pedido);
                System.out.println("DEBUG: Estado cambiado a PAGADO");
            }

            // vaciar carrito
            session.removeAttribute("carrito");
            System.out.println("DEBUG: Carrito eliminado de sesión");

            model.addAttribute("pedido", pedido);
            model.addAttribute("origenPago", origen != null ? origen : "directo");

            System.out.println("DEBUG: Mostrando confirmacion.html");
            return "confirmacion";

        } catch (Exception e) {
            System.err.println("❌ Error en pagarPedido: " + e.getMessage());
            return "redirect:/carrito?error=" + e.getMessage();
        }
    }

    // Crear endpoints específicos para AJAX
    @PostMapping("/crear-ajax")
    @ResponseBody
    public Map<String, Object> crearPedidoAjax(HttpSession session, Model model) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            // Ejecutar la lógica normal
            procesarCrearPedido(session, model);

            // Obtener el pedido recién creado del modelo
            Pedido pedido = (Pedido) model.getAttribute("pedido");

            if (pedido != null) {
                respuesta.put("success", true);
                respuesta.put("pedido", Map.of(
                        "id", pedido.getId(),
                        "fecha", pedido.getFecha().toString(),
                        "estado", pedido.getEstado(),
                        "total", pedido.getTotal(),
                        "detalles", pedido.getDetalles().stream().map(d -> Map.of(
                                "producto", d.getProducto().getNombre(),
                                "cantidad", d.getCantidad(),
                                "precio", d.getPrecioUnitario(),
                                "subtotal", d.getCantidad() * d.getPrecioUnitario()
                        )).collect(Collectors.toList())
                ));
            } else {
                respuesta.put("success", false);
                respuesta.put("error", "No se pudo crear el pedido");
            }

        } catch (Exception e) {
            respuesta.put("success", false);
            respuesta.put("error", e.getMessage());
        }

        return respuesta;
    }

    @PostMapping("/pagar-ajax/{id}")
    @ResponseBody
    public Map<String, Object> pagarPedidoAjax(@PathVariable Long id, HttpSession session) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            Pedido pedido = pedidoRepository.findById(id).orElseThrow();
            pedido.setEstado("PAGADO");
            pedidoRepository.save(pedido);

            // vaciar carrito
            session.removeAttribute("carrito");

            respuesta.put("success", true);
            respuesta.put("pedido", Map.of(
                    "id", pedido.getId(),
                    "estado", pedido.getEstado(),
                    "total", pedido.getTotal()
            ));

        } catch (Exception e) {
            respuesta.put("success", false);
            respuesta.put("error", e.getMessage());
        }

        return respuesta;
    }

    @GetMapping("/crear")
    public String mostrarFormulario() {
        return "redirect:/carrito"; // O muestra una página de confirmación
    }

    // Lógica común para crear pedido
    private String procesarCrearPedido(HttpSession session, Model model) {
        System.out.println("\n=== 🎯 INICIO procesarCrearPedido ===");
        System.out.println("Sesión ID: " + session.getId());

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
        System.out.println("3b. Tipo de carritoObj: " + (carritoObj != null ? carritoObj.getClass().getName() : "null"));

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

                    System.out.println("   Procesando item - Key: " + keyObj + ", Value: " + valueObj);

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
        } else {
            System.out.println("⚠️ carritoObj no es un Map, es: " + (carritoObj != null ? carritoObj.getClass().getName() : "null"));
        }

        System.out.println("5. Carrito procesado tamaño: " + carrito.size());

        if (carrito == null || carrito.isEmpty()) {
            System.out.println("❌ ERROR: Carrito vacío, redirigiendo a /carrito");
            model.addAttribute("error", "El carrito está vacío");
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
            System.out.println("7. Retornando 'pago'");
            return "pago";

        } catch (Exception e) {
            System.out.println("❌ ERROR AL CREAR PEDIDO: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al crear el pedido: " + e.getMessage());
            return "redirect:/carrito";
        }
    }



}