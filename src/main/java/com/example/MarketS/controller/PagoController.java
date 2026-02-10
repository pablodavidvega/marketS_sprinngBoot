package com.example.MarketS.controller;
import com.example.MarketS.config.MercadoPagoConfig;
import com.example.MarketS.service.MercadoPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/pago")
public class PagoController {

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Autowired
    private MercadoPagoConfig config;

    /**
     * Página para probar pago
     */
    @GetMapping("/probar")
    public String paginaPrueba(Model model) {
        model.addAttribute("pedidoId", System.currentTimeMillis() % 1000000);
        model.addAttribute("monto", 15000.0);
        model.addAttribute("descripcion", "Compra de prueba MarketS");
        model.addAttribute("email", "cliente@test.com");
        return "pago/prueba";
    }

    /**
     * Probar conexión con MercadoPago
     */
    @GetMapping("/test-conexion")
    @ResponseBody
    public String testConexion() {
        return mercadoPagoService.pruebaConexion();
    }

    /**
     * Crear y redirigir a pago
     */
    @PostMapping("/crear")
    public String crearPago(@RequestParam Long pedidoId,
                            @RequestParam Double monto,
                            @RequestParam String descripcion,
                            @RequestParam String email,
                            Model model) {

        Map<String, Object> resultado = mercadoPagoService.crearPreferencia(
                pedidoId, monto, descripcion, email
        );

        if (Boolean.TRUE.equals(resultado.get("success"))) {
            return "redirect:" + resultado.get("paymentUrl");
        } else {
            model.addAttribute("error", resultado.get("error"));
            return "pago/error";
        }
    }

    /**
     * Endpoint SIMPLE para prueba rápida
     */
    @GetMapping("/pago-rapido")
    public String pagoRapido() {
        Long pedidoId = System.currentTimeMillis() % 1000000;

        Map<String, Object> resultado = mercadoPagoService.crearPreferencia(
                pedidoId,
                15000.0,
                "Pago rápido MarketS",
                "test@test.com"
        );

        if (Boolean.TRUE.equals(resultado.get("success"))) {
            return "redirect:" + resultado.get("paymentUrl");
        } else {
            return "redirect:/pago/error?msg=" + resultado.get("error");
        }
    }

    /**
     * Callback de éxito
     */
    @GetMapping("/exito")
    @ResponseBody
    public String exitoPago(@RequestParam Map<String, String> params) {
        System.out.println("\n🎉🎉🎉 PAGO EXITOSO 🎉🎉🎉");
        System.out.println("Parámetros: " + params);

        String html = "<html><body style='text-align: center; padding: 50px;'>";
        html += "<h1 style='color: green;'>✅ ¡PAGO EXITOSO!</h1>";
        html += "<p>Transaction ID: " + params.get("payment_id") + "</p>";
        html += "<p>Status: " + params.get("status") + "</p>";
        html += "<p>External Reference: " + params.get("external_reference") + "</p>";
        html += "<br><a href='/pago/probar'>Probar otro pago</a>";
        html += "</body></html>";

        return html;
    }

    /**
     * Callback de error
     */
    @GetMapping("/error")
    @ResponseBody
    public String errorPago(@RequestParam(required = false) String msg) {
        String html = "<html><body style='text-align: center; padding: 50px;'>";
        html += "<h1 style='color: red;'>❌ ERROR EN EL PAGO</h1>";
        if (msg != null) html += "<p>" + msg + "</p>";
        html += "<br><a href='/pago/probar'>Intentar de nuevo</a>";
        html += "</body></html>";

        return html;
    }

    @GetMapping("/debug-config")
    @ResponseBody
    public String debugConfig() {
        String token = config.getAccessToken();
        String appUrl = config.getAppUrl();

        String resultado = "<h1>Debug Configuracion</h1>" +
                "<p>Token (primeros 20): " + (token != null ? token.substring(0, Math.min(20, token.length())) : "NULL") + "</p>" +
                "<p>App URL: " + appUrl + "</p>" +
                "<p>Token empieza con APP_USR: " + (token != null && token.startsWith("APP_USR")) + "</p>" +
                "<p>Token empieza con TEST: " + (token != null && token.startsWith("TEST")) + "</p>";

        return resultado;
    }

    @GetMapping("/test-pago-simple")
    @ResponseBody
    public String testPagoSimple() {
        try {
            Long pedidoId = System.currentTimeMillis() % 1000000;
            Map<String, Object> resultado = mercadoPagoService.crearPreferencia(
                    pedidoId,
                    15000.0,
                    "Test Pago Simple",
                    "test@test.com"
            );

            if (Boolean.TRUE.equals(resultado.get("success"))) {
                String url = (String) resultado.get("paymentUrl");
                return "<h1>Pago creado exitosamente</h1>" +
                        "<p>URL: <a href='" + url + "'>" + url + "</a></p>" +
                        "<p><a href='" + url + "'>Ir a MercadoPago</a></p>";
            } else {
                return "<h1>Error creando pago</h1><p>" + resultado.get("error") + "</p>";
            }
        } catch (Exception e) {
            return "<h1>Error</h1><p>" + e.getMessage() + "</p>";
        }
    }
}