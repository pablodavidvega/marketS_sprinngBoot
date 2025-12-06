package com.example.MarketS.service.pdf;

import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

import java.util.Base64;

public class B64ImgReplacedElementFactory implements ReplacedElementFactory {

    @Override
    public ReplacedElement createReplacedElement(
            LayoutContext layoutContext,
            BlockBox box,
            UserAgentCallback userAgentCallback,
            int cssWidth,
            int cssHeight) {

        Element element = box.getElement();
        if (element == null) return null;

        if (!"img".equalsIgnoreCase(element.getNodeName())) return null;

        String src = element.getAttribute("src");
        if (src == null || !src.startsWith("data:image")) return null;

        try {
            // quitar encabezado data:image/png;base64,
            String base64 = src.substring(src.indexOf(",") + 1);

            // cargar imagen
            ITextFSImage image = (ITextFSImage) userAgentCallback
                    .getImageResource("data:image/png;base64," + base64)
                    .getImage();

            if (image != null) {
                if (cssWidth > 0 && cssHeight > 0) {
                    image.scale(cssWidth, cssHeight);
                }
                return new ITextImageElement(image);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override public void reset() {}
    @Override public void remove(Element element) {}
    @Override public void setFormSubmissionListener(FormSubmissionListener listener) {}
}
