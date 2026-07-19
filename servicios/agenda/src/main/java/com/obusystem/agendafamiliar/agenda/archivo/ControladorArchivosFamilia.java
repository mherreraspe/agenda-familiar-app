package com.obusystem.agendafamiliar.agenda.archivo;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorArchivosFamilia {
    private final ServicioArchivosFamilia servicio;

    public ControladorArchivosFamilia(ServicioArchivosFamilia servicio) { this.servicio = servicio; }

    @PostMapping(path = "/tratamientos/{tratamientoId}/receta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<RespuestaArchivo> subir(@PathVariable UUID familiaId, @PathVariable UUID tratamientoId,
            @RequestPart("archivo") MultipartFile archivo, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(servicio.subirReceta(familiaId, tratamientoId, archivo, jwt));
    }

    @GetMapping("/archivos/{archivoId}")
    ResponseEntity<byte[]> descargar(@PathVariable UUID familiaId, @PathVariable UUID archivoId,
            @RequestParam(defaultValue = "false") boolean miniatura, @AuthenticationPrincipal Jwt jwt) {
        ContenidoArchivo archivo = servicio.descargar(familiaId, archivoId, miniatura, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receta-" + archivo.id() + ".jpg")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_JPEG)
                .body(archivo.contenido());
    }

    @DeleteMapping("/archivos/{archivoId}")
    ResponseEntity<Void> eliminar(@PathVariable UUID familiaId, @PathVariable UUID archivoId,
            @AuthenticationPrincipal Jwt jwt) {
        servicio.eliminar(familiaId, archivoId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archivos/cuota")
    RespuestaCuota cuota(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.cuota(familiaId, jwt);
    }
}
