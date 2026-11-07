package com.monolito.historial.controller;

import com.monolito.historial.model.Adjunto;
import com.monolito.historial.security.UsuarioPrincipal;
import com.monolito.historial.service.AdjuntoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/vehiculos/{vehiculoId}/intervenciones/{intervencionId}/adjuntos")
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    public AdjuntoController(AdjuntoService adjuntoService) {
        this.adjuntoService = adjuntoService;
    }

    // GET .../adjuntos/{adjuntoId}  -> abre el PDF o la imagen en el navegador
    @GetMapping("/{adjuntoId:[0-9]+}")
    public ResponseEntity<byte[]> ver(@PathVariable Long vehiculoId,
                                      @PathVariable Long intervencionId,
                                      @PathVariable Long adjuntoId,
                                      @AuthenticationPrincipal UsuarioPrincipal usuario) {

        Adjunto adjunto = adjuntoService.obtenerDelUsuario(usuario.getId(), vehiculoId, intervencionId, adjuntoId);
        byte[] datos = adjuntoService.obtenerDatos(adjunto);

        // El tipo sale de la base de datos (ya comprobado al subirlo): solo PDF o imágenes
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(adjunto.getTipoContenido()))
                .contentLength(datos.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(adjunto.getNombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .body(datos);
    }

    // POST .../adjuntos/{adjuntoId}/eliminar
    @PostMapping("/{adjuntoId:[0-9]+}/eliminar")
    public String eliminar(@PathVariable Long vehiculoId,
                           @PathVariable Long intervencionId,
                           @PathVariable Long adjuntoId,
                           @AuthenticationPrincipal UsuarioPrincipal usuario,
                           RedirectAttributes redirectAttributes) {

        Adjunto adjunto = adjuntoService.obtenerDelUsuario(usuario.getId(), vehiculoId, intervencionId, adjuntoId);
        adjuntoService.eliminar(adjunto);

        redirectAttributes.addFlashAttribute("mensaje", "Archivo eliminado.");
        return "redirect:/vehiculos/" + vehiculoId;
    }
}
