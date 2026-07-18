package com.obusystem.agendafamiliar.agenda.archivo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlmacenCifrado {
    private static final int TAMANO_IV = 12;
    private final Path raiz;
    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public AlmacenCifrado(@Value("${archivos.raiz}") String raiz,
            @Value("${archivos.clave}") String claveBase64) {
        this.raiz = Path.of(raiz).toAbsolutePath().normalize();
        byte[] bytes = Base64.getDecoder().decode(claveBase64);
        if (bytes.length != 32) throw new IllegalStateException("ARCHIVOS_ENCRYPTION_KEY debe contener 32 bytes en Base64");
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public long guardar(String rutaRelativa, byte[] contenido) {
        Path destino = resolver(rutaRelativa);
        Path temporal = null;
        try {
            Files.createDirectories(destino.getParent());
            byte[] cifrado = cifrar(contenido);
            temporal = Files.createTempFile(destino.getParent(), ".subida-", ".tmp");
            Files.write(temporal, cifrado);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException error) {
                Files.move(temporal, destino);
            }
            return cifrado.length;
        } catch (IOException | GeneralSecurityException error) {
            if (temporal != null) eliminarSilencioso(temporal);
            throw new IllegalStateException("No se pudo guardar el archivo cifrado", error);
        }
    }

    public byte[] leer(String rutaRelativa) {
        try {
            byte[] contenido = Files.readAllBytes(resolver(rutaRelativa));
            if (contenido.length <= TAMANO_IV + 16) throw new IllegalStateException("Archivo cifrado incompleto");
            byte[] iv = java.util.Arrays.copyOfRange(contenido, 0, TAMANO_IV);
            byte[] cifrado = java.util.Arrays.copyOfRange(contenido, TAMANO_IV, contenido.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(128, iv));
            return cipher.doFinal(cifrado);
        } catch (IOException | GeneralSecurityException error) {
            throw new IllegalStateException("No se pudo leer el archivo cifrado", error);
        }
    }

    public void eliminar(String rutaRelativa) { eliminarSilencioso(resolver(rutaRelativa)); }

    private byte[] cifrar(byte[] contenido) throws GeneralSecurityException {
        byte[] iv = new byte[TAMANO_IV];
        aleatorio.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(contenido);
        return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
    }

    private Path resolver(String rutaRelativa) {
        Path ruta = raiz.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(raiz)) throw new IllegalArgumentException("Ruta de archivo inválida");
        return ruta;
    }

    private void eliminarSilencioso(Path ruta) {
        try { Files.deleteIfExists(ruta); } catch (IOException ignorado) { }
    }
}
