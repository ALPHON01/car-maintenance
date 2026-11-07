package com.monolito.historial.controller;

import com.monolito.historial.dto.ArchivoSubido;
import com.monolito.historial.dto.IntervencionForm;
import com.monolito.historial.exception.ArchivoInvalidoException;
import com.monolito.historial.model.Vehiculo;
import com.monolito.historial.repository.VehiculoRepository;
import com.monolito.historial.security.UsuarioPrincipal;
import com.monolito.historial.service.AdjuntoService;
import com.monolito.historial.service.MantenimientoService;
import com.monolito.historial.util.Formularios;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vehiculos/{vehiculoId}/pendientes")
public class PendienteController {

    private final VehiculoRepository vehiculoRepository;
    private final MantenimientoService mantenimientoService;
    private final AdjuntoService adjuntoService;

    public PendienteController(VehiculoRepository vehiculoRepository,
                               MantenimientoService mantenimientoService,
                               AdjuntoService adjuntoService) {
        this.vehiculoRepository = vehiculoRepository;
        this.mantenimientoService = mantenimientoService;
        this.adjuntoService = adjuntoService;
    }

    // POST /vehiculos/{vehiculoId}/pendientes/{pendienteId}/completar
    @PostMapping("/{pendienteId:[0-9]+}/completar")
    public String completar(@PathVariable Long vehiculoId,
                            @PathVariable Long pendienteId,
                            @Valid @ModelAttribute("intervencionForm") IntervencionForm form,
                            BindingResult result,
                            @RequestParam(name = "archivos", required = false) List<MultipartFile> archivos,
                            @AuthenticationPrincipal UsuarioPrincipal usuario,
                            RedirectAttributes redirectAttributes) {

        Vehiculo vehiculo = obtenerVehiculoDelUsuario(vehiculoId, usuario);

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorFormulario", Formularios.resumenErrores(result));
            return "redirect:/vehiculos/" + vehiculoId;
        }

        try {
            List<ArchivoSubido> adjuntos = adjuntoService.preparar(archivos);
            mantenimientoService.completarPendiente(vehiculo, pendienteId, form, adjuntos);
        } catch (ArchivoInvalidoException e) {
            redirectAttributes.addFlashAttribute("errorFormulario", e.getMessage());
            return "redirect:/vehiculos/" + vehiculoId;
        }

        redirectAttributes.addFlashAttribute("mensaje",
                "Mantenimiento marcado como realizado y añadido al historial.");
        return "redirect:/vehiculos/" + vehiculoId;
    }

    private Vehiculo obtenerVehiculoDelUsuario(Long vehiculoId, UsuarioPrincipal usuario) {
        return vehiculoRepository.findByIdAndUsuarioId(vehiculoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }
}
