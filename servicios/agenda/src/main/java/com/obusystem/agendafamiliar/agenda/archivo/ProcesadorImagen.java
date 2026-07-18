package com.obusystem.agendafamiliar.agenda.archivo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProcesadorImagen {
    static final long MAXIMO_BYTES = 12L * 1024 * 1024;
    static final long MAXIMO_PIXELES = 40_000_000L;
    private static final int LADO_ORIGINAL = 1600;
    private static final int LADO_MINIATURA = 320;

    public ImagenProcesada procesar(byte[] fuente, String mimeDeclarado) {
        if (fuente == null || fuente.length == 0 || fuente.length > MAXIMO_BYTES) {
            throw problema(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen debe pesar entre 1 byte y 12 MiB");
        }
        if (!"image/jpeg".equals(mimeDeclarado) && !"image/png".equals(mimeDeclarado)) {
            throw problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo se aceptan imágenes JPEG o PNG");
        }
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(fuente))) {
            if (entrada == null) throw problema(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw problema(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                String formato = lector.getFormatName().toUpperCase(Locale.ROOT);
                if (!formato.equals("JPEG") && !formato.equals("JPG") && !formato.equals("PNG")) {
                    throw problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El contenido no es JPEG ni PNG");
                }
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho > 12_000 || alto > 12_000 || (long) ancho * alto > MAXIMO_PIXELES) {
                    throw problema(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el máximo de 40 megapíxeles");
                }
                BufferedImage imagen = lector.read(0);
                BufferedImage normalizada = escalar(imagen, LADO_ORIGINAL);
                BufferedImage miniatura = escalar(normalizada, LADO_MINIATURA);
                byte[] original = escribirJpeg(normalizada, .86f);
                byte[] mini = escribirJpeg(miniatura, .78f);
                return new ImagenProcesada(original, mini, normalizada.getWidth(), normalizada.getHeight(), sha256(original));
            } finally {
                lector.dispose();
            }
        } catch (ResponseStatusException error) {
            throw error;
        } catch (IOException error) {
            throw problema(HttpStatus.BAD_REQUEST, "No se pudo interpretar la imagen");
        }
    }

    private BufferedImage escalar(BufferedImage fuente, int maximo) {
        double factor = Math.min(1d, Math.min((double) maximo / fuente.getWidth(), (double) maximo / fuente.getHeight()));
        int ancho = Math.max(1, (int) Math.round(fuente.getWidth() * factor));
        int alto = Math.max(1, (int) Math.round(fuente.getHeight() * factor));
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = destino.createGraphics();
        try {
            graficos.setColor(Color.WHITE);
            graficos.fillRect(0, 0, ancho, alto);
            graficos.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graficos.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graficos.drawImage(fuente, 0, 0, ancho, alto, null);
        } finally {
            graficos.dispose();
        }
        return destino;
    }

    private byte[] escribirJpeg(BufferedImage imagen, float calidad) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ImageOutputStream imagenSalida = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(imagenSalida);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(calidad);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
            imagenSalida.flush();
            return salida.toByteArray();
        } finally {
            escritor.dispose();
        }
    }

    private String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private ResponseStatusException problema(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
