package com.example.MarketS.service;

import com.example.MarketS.model.Categorias;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.CategoriaRepository;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.MarketS.model.Producto;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;

import com.lowagie.text.Image;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

@Service
public class PdfThymeleafService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CategoriaRepository categoriaRepository;


    // Conversión seguro gráfico -> Base64
    private String chartToBase64(JFreeChart chart, int width, int height) {
        try {
            BufferedImage image = chart.createBufferedImage(width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Generar PDF
    public byte[] generarPdfDesdeHtml(List<Producto> productos, User usuario) {

        // 1. Inicializar todas las categorías en 0
        Map<String, Long> productosPorCategoria = categoriaRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Categorias::getNombreCategoria,
                        c -> 0L
                ));

// 2. Contar productos existentes
        productos.forEach(p -> {
            if (p.getCategorias() != null) {
                String nombre = p.getCategorias().getNombreCategoria();
                productosPorCategoria.put(
                        nombre,
                        productosPorCategoria.get(nombre) + 1
                );
            }
        });


        DefaultCategoryDataset datasetBarras = new DefaultCategoryDataset();
        productosPorCategoria.forEach((cat, count) ->
                datasetBarras.addValue(count, "Productos", cat)
        );

        JFreeChart graficoBarras = ChartFactory.createBarChart(
                "Productos por Categoría", "Categoría", "Cantidad", datasetBarras
        );

        String barrasBase64 = chartToBase64(graficoBarras, 600, 400);


        // Torta activos / inactivos
        long activos = productos.stream().filter(Producto::getActivo).count();
        long inactivos = productos.size() - activos;

        DefaultPieDataset<String> datasetTorta = new DefaultPieDataset<>();
        datasetTorta.setValue("Activos", activos);
        datasetTorta.setValue("Inactivos", inactivos);

        JFreeChart graficoTorta = ChartFactory.createPieChart(
                "Productos Activos vs Inactivos", datasetTorta, true, true, false
        );

        String tortaBase64 = chartToBase64(graficoTorta, 500, 350);

        // Enviar al HTML
        Context context = new Context();
        context.setVariable("productos", productos);
        context.setVariable("usuario", usuario);
        context.setVariable("graficoBarras", barrasBase64);
        context.setVariable("graficoTorta", tortaBase64);

        String html = templateEngine.process("reporte", context);
        System.out.println("HTML generado:\n" + html);

        // Render PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ITextRenderer renderer = new ITextRenderer();

            // Usar la clase interna en lugar de la externa faltante
            renderer.getSharedContext().setReplacedElementFactory(
                    new InternalB64ImgReplacedElementFactory()
            );

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("HTML enviado:");
            System.out.println(html);

            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    // Clase interna adaptada para la versión específica de Flying Saucer
    private static class InternalB64ImgReplacedElementFactory implements ReplacedElementFactory {

        @Override
        public ReplacedElement createReplacedElement(LayoutContext lc, BlockBox box,
                                                     UserAgentCallback uac, int cssWidth, int cssHeight) {

            org.w3c.dom.Element element = box.getElement();
            if (element == null) {
                return null;
            }

            String nodeName = element.getNodeName();
            String src = element.getAttribute("src");

            // Procesar imágenes en base64
            if ("img".equals(nodeName) && src != null && src.startsWith("data:image")) {
                try {
                    // Extraer base64
                    String base64Data = src.substring(src.indexOf("base64,") + 7);
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                    // Crear imagen
                    Image image = Image.getInstance(imageBytes);
                    FSImage fsImage = new ITextFSImage(image);

                    // Escalar si es necesario
                    if (cssWidth > 0 || cssHeight > 0) {
                        fsImage.scale(cssWidth, cssHeight);
                    }

                    return new ITextImageElement(fsImage);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return null;
        }

        @Override
        public void reset() {}

        @Override
        public void remove(org.w3c.dom.Element e) {}

        // Añadir este método específico para tu versión de Flying Saucer
        @Override
        public void setFormSubmissionListener(org.xhtmlrenderer.simple.extend.FormSubmissionListener listener) {
            // No es necesario implementar nada aquí
        }
    }

}