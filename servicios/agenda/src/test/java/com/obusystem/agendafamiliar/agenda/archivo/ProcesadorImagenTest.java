package com.obusystem.agendafamiliar.agenda.archivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

public class ProcesadorImagenTest {
    private final ProcesadorImagen procesador = new ProcesadorImagen();

    @Test
    void reduceReencodeaYEliminaDatosAnexos() throws Exception {
        byte[] marca = "GPS-SECRET-EXIF".getBytes(StandardCharsets.UTF_8);
        byte[] fuente = imagenPng(2000, 1000);
        byte[] contaminada = new byte[fuente.length + marca.length];
        System.arraycopy(fuente, 0, contaminada, 0, fuente.length);
        System.arraycopy(marca, 0, contaminada, fuente.length, marca.length);

        ImagenProcesada resultado = procesador.procesar(contaminada, "image/png");

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(resultado.original()));
        BufferedImage miniatura = ImageIO.read(new ByteArrayInputStream(resultado.miniatura()));
        assertThat(original.getWidth()).isEqualTo(1600);
        assertThat(original.getHeight()).isEqualTo(800);
        assertThat(miniatura.getWidth()).isEqualTo(320);
        assertThat(new String(resultado.original(), StandardCharsets.ISO_8859_1)).doesNotContain("GPS-SECRET-EXIF");
        assertThat(resultado.sha256()).hasSize(64);
    }

    @Test
    void rechazaTipoDeclaradoYContenidoInvalidos() {
        assertThatThrownBy(() -> procesador.procesar(new byte[] {1, 2, 3}, "image/svg+xml"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("JPEG o PNG");
        assertThatThrownBy(() -> procesador.procesar(new byte[] {1, 2, 3}, "image/jpeg"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no es válida");
    }

    public static byte[] imagenPng(int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        var graficos = imagen.createGraphics();
        graficos.setColor(new Color(49, 91, 76));
        graficos.fillRect(0, 0, ancho, alto);
        graficos.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
