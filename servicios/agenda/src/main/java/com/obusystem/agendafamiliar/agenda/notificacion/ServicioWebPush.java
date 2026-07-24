package com.obusystem.agendafamiliar.agenda.notificacion;

import java.security.GeneralSecurityException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

@Service
public class ServicioWebPush {
    private static final String MENSAJE_GENERICO = "{\"titulo\":\"Agenda Familiar\",\"cuerpo\":\"Tienes algo por revisar.\",\"destino\":\"/hoy?avisos=1\"}";

    private final String clavePublica;
    private final PushService servicio;

    public ServicioWebPush(
            @Value("${notificaciones.web-push.clave-publica:}") String clavePublica,
            @Value("${notificaciones.web-push.clave-privada:}") String clavePrivada,
            @Value("${notificaciones.web-push.sujeto:mailto:administracion@obusystem.com}") String sujeto) {
        this.clavePublica = clavePublica == null ? "" : clavePublica.trim();
        String privada = clavePrivada == null ? "" : clavePrivada.trim();
        if (this.clavePublica.isEmpty() || privada.isEmpty()) {
            this.servicio = null;
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            this.servicio = new PushService(this.clavePublica, privada, sujeto);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Las claves VAPID configuradas no son válidas", error);
        }
    }

    public boolean disponible() {
        return servicio != null;
    }

    public String clavePublica() {
        return disponible() ? clavePublica : "";
    }

    public Resultado enviar(String endpoint, String claveP256dh, String claveAuth) {
        if (!disponible()) return Resultado.NO_CONFIGURADO;
        try {
            var respuesta = servicio.send(new Notification(endpoint, claveP256dh, claveAuth, MENSAJE_GENERICO));
            int estado = respuesta.getStatusLine().getStatusCode();
            if (estado >= 200 && estado < 300) return Resultado.ENVIADO;
            if (estado == 404 || estado == 410) return Resultado.EXPIRADO;
            return Resultado.ERROR;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return Resultado.ERROR;
        } catch (Exception error) {
            return Resultado.ERROR;
        }
    }

    public enum Resultado { ENVIADO, EXPIRADO, ERROR, NO_CONFIGURADO }
}
