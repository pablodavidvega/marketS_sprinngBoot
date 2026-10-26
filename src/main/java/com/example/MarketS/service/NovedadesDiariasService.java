package com.example.MarketS.service;

import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

// Resumen diario "Novedades del día": un solo correo por persona con los productos publicados
// desde el último envío, en vez de un correo por producto — con el límite de 100 correos/día
// de Resend Free, un correo por producto agotaría el cupo en pocas publicaciones y dejaría sin
// enviar los correos críticos (recuperar contraseña, pedidos), que comparten ese mismo cupo.
@Service
public class NovedadesDiariasService {

    private static final Logger logger = LoggerFactory.getLogger(NovedadesDiariasService.class);

    // Productos que se listan en el correo; el resto se resume en "...y N más" + botón
    private static final int MAX_PRODUCTOS_CORREO = 12;
    // Resend acepta 2 envíos por segundo: con esta pausa el resumen nunca choca con ese límite
    private static final long PAUSA_ENTRE_ENVIOS_MS = 600;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProductoRepository productoRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FrontendLinkBuilder frontendLinkBuilder;

    @Value("${app.url:http://localhost:8081}")
    private String appUrl;

    // Apagado por defecto: mientras el dominio no esté verificado en Resend, los envíos a
    // cualquiera que no sea el dueño de la cuenta fallan, y el envío automático marcaría los
    // productos como anunciados sin que nadie los reciba. El botón del admin funciona igual,
    // para probar. Se enciende con NOVEDADES_HABILITADO=true en Render.
    @Value("${novedades.habilitado:false}")
    private boolean habilitado;

    public record Resultado(int productos, int enviados, int fallidos) {}

    private record Novedad(Long vendedorId, EmailService.ProductoNovedad datos) {}

    public NovedadesDiariasService(ProductoRepository productoRepository, UserRepository userRepository,
                                   EmailService emailService, FrontendLinkBuilder frontendLinkBuilder) {
        this.productoRepository = productoRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendLinkBuilder = frontendLinkBuilder;
    }

    // 7 p. m. hora de Colombia: después de la jornada, cuando la gente revisa el correo
    @Scheduled(cron = "${novedades.cron:0 0 19 * * *}", zone = "America/Bogota")
    public void enviarProgramado() {
        if (!habilitado) return;
        Resultado r = enviarResumen();
        logger.info("Resumen de novedades: {} productos, {} correos enviados, {} fallidos",
                r.productos(), r.enviados(), r.fallidos());
    }

    // synchronized: el envío programado y el botón del admin no pueden correr a la vez y
    // anunciar dos veces los mismos productos.
    public synchronized Resultado enviarResumen() {
        List<Producto> pendientes = productoRepository.findNovedadesPendientes();
        if (pendientes.isEmpty()) {
            return new Resultado(0, 0, 0);
        }

        // Se arma todo lo que el correo necesita antes de marcar: marcarNovedadesNotificadas
        // limpia el contexto de persistencia. El id del vendedor se lee del proxy LAZY sin
        // consultar la base (ver comentario en Producto.vendedor).
        List<Novedad> novedades = pendientes.stream()
                .map(p -> new Novedad(
                        p.getVendedor() != null ? p.getVendedor().getId() : null,
                        new EmailService.ProductoNovedad(
                                p.getNombre(),
                                p.getPrecio(),
                                urlAbsoluta(p.getImagenThumb() != null ? p.getImagenThumb() : p.getImagen()),
                                frontendLinkBuilder.detalleProducto(p.getId()))))
                .toList();

        // Se marcan antes de enviar: si el envío se corta a mitad de camino, los productos no
        // se vuelven a anunciar mañana a quienes ya los recibieron. Es preferible que alguien
        // se pierda un resumen a que le lleguen repetidos.
        productoRepository.marcarNovedadesNotificadas(pendientes.stream().map(Producto::getId).toList());

        int enviados = 0;
        int fallidos = 0;
        for (Object[] fila : userRepository.findSuscritosNovedades()) {
            Long userId = ((Number) fila[0]).longValue();
            String username = (String) fila[1];
            String email = (String) fila[2];
            String token = (String) fila[3];

            // Un vendedor no necesita enterarse de sus propios productos
            List<EmailService.ProductoNovedad> paraEsteUsuario = novedades.stream()
                    .filter(n -> !Objects.equals(n.vendedorId(), userId))
                    .map(Novedad::datos)
                    .toList();
            if (paraEsteUsuario.isEmpty()) continue;

            if (token == null) {
                token = nuevoToken();
                userRepository.asignarTokenBajaNovedades(userId, token);
            }

            boolean ok = emailService.enviarNovedades(
                    email,
                    username,
                    paraEsteUsuario.subList(0, Math.min(MAX_PRODUCTOS_CORREO, paraEsteUsuario.size())),
                    paraEsteUsuario.size(),
                    frontendLinkBuilder.productosRecientes(),
                    frontendLinkBuilder.bajaNovedades(token),
                    appUrl + "/api/novedades/baja?token=" + token);
            if (ok) enviados++;
            else fallidos++;

            pausa();
        }
        return new Resultado(pendientes.size(), enviados, fallidos);
    }

    // Los clientes de correo solo muestran imágenes con URL absoluta: los archivos viejos
    // guardados en /uploads del propio servidor (anteriores a Supabase Storage) se omiten.
    private static String urlAbsoluta(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://")) ? url : null;
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void pausa() {
        try {
            Thread.sleep(PAUSA_ENTRE_ENVIOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
