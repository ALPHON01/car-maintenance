package com.monolito.historial.controller;

import com.monolito.historial.dto.ArchivoSubido;
import com.monolito.historial.dto.IntervencionForm;
import com.monolito.historial.exception.ArchivoInvalidoException;
import com.monolito.historial.model.TipoMantenimiento;
import com.monolito.historial.model.Vehiculo;
import com.monolito.historial.repository.VehiculoRepository;
import com.monolito.historial.security.UsuarioPrincipal;
import com.monolito.historial.service.AdjuntoService;
import com.monolito.historial.service.MantenimientoService;
import com.monolito.historial.service.TallerService;
import com.monolito.historial.util.Formularios;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/vehiculos/{vehiculoId}/intervenciones")
public class IntervencionController {

    private final VehiculoRepository vehiculoRepository;
    private final TallerService tallerService;
    private final MantenimientoService mantenimientoService;
    private final AdjuntoService adjuntoService;

    public IntervencionController(VehiculoRepository vehiculoRepository,
                                  TallerService tallerService,
                                  MantenimientoService mantenimientoService,
                                  AdjuntoService adjuntoService) {
        this.vehiculoRepository = vehiculoRepository;
        this.tallerService = tallerService;
        this.mantenimientoService = mantenimientoService;
        this.adjuntoService = adjuntoService;
    }

    // GET /vehiculos/{vehiculoId}/intervenciones/nueva
    @GetMapping("/nueva")
    public String formularioNueva(@PathVariable Long vehiculoId,
                                  @AuthenticationPrincipal UsuarioPrincipal usuario,
                                  Model model) {
        Vehiculo vehiculo = obtenerVehiculoDelUsuario(vehiculoId, usuario);

        IntervencionForm form = new IntervencionForm();
        form.setFecha(LocalDate.now());

        model.addAttribute("intervencionForm", form);
        cargarFormulario(model, vehiculo, usuario);
        return "intervenciones/formulario";
    }

    // POST /vehiculos/{vehiculoId}/intervenciones
    // Si no se elige taller (tallerId vacío), el servicio asigna el taller de confianza.
    @PostMapping
    public String guardar(@PathVariable Long vehiculoId,
                          @Valid @ModelAttribute("intervencionForm") IntervencionForm form,
                          BindingResult result,
                          @RequestParam(name = "archivos", required = false) List<MultipartFile> archivos,
                          @AuthenticationPrincipal UsuarioPrincipal usuario,
                          Model model) {

        Vehiculo vehiculo = obtenerVehiculoDelUsuario(vehiculoId, usuario);

        if (form.getTipo() == null && !result.hasFieldErrors("tipo")) {
            result.rejectValue("tipo", "tipo.obligatorio", "Selecciona el tipo de mantenimiento");
        }

        // Los archivos se validan antes de guardar nada
        List<ArchivoSubido> adjuntos = List.of();
        try {
            adjuntos = adjuntoService.preparar(archivos);
        } catch (ArchivoInvalidoException e) {
            result.reject("archivos.invalidos", e.getMessage());
        }

        if (result.hasErrors()) {
            cargarFormulario(model, vehiculo, usuario);
            return "intervenciones/formulario";
        }

        try {
            mantenimientoService.registrarIntervencion(vehiculo, form, adjuntos);
        } catch (ArchivoInvalidoException e) {
            result.reject("archivos.invalidos", e.getMessage());
            cargarFormulario(model, vehiculo, usuario);
            return "intervenciones/formulario";
        }
        return "redirect:/vehiculos/" + vehiculoId;
    }

    // POST /vehiculos/{vehiculoId}/intervenciones/{intervencionId}/editar
    @PostMapping("/{intervencionId:[0-9]+}/editar")
    public String editar(@PathVariable Long vehiculoId,
                         @PathVariable Long intervencionId,
                         @Valid @ModelAttribute("intervencionForm") IntervencionForm form,
                         BindingResult result,
                         @RequestParam(name = "archivos", required = false) List<MultipartFile> archivos,
                         @AuthenticationPrincipal UsuarioPrincipal usuario,
                         RedirectAttributes redirectAttributes) {

        Vehiculo vehiculo = obtenerVehiculoDelUsuario(vehiculoId, usuario);

        if (form.getTipo() == null && !result.hasFieldErrors("tipo")) {
            result.rejectValue("tipo", "tipo.obligatorio", "Selecciona el tipo de mantenimiento");
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorFormulario", Formularios.resumenErrores(result));
            return "redirect:/vehiculos/" + vehiculoId;
        }

        try {
            List<ArchivoSubido> adjuntos = adjuntoService.preparar(archivos);
            mantenimientoService.actualizarIntervencion(vehiculo, intervencionId, form, adjuntos);
        } catch (ArchivoInvalidoException e) {
            redirectAttributes.addFlashAttribute("errorFormulario", e.getMessage());
            return "redirect:/vehiculos/" + vehiculoId;
        }

        redirectAttributes.addFlashAttribute("mensaje", "Visita actualizada correctamente.");
        return "redirect:/vehiculos/" + vehiculoId;
    }

    private Vehiculo obtenerVehiculoDelUsuario(Long vehiculoId, UsuarioPrincipal usuario) {
        return vehiculoRepository.findByIdAndUsuarioId(vehiculoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    private void cargarFormulario(Model model, Vehiculo vehiculo, UsuarioPrincipal usuario) {
        model.addAttribute("vehiculo", vehiculo);
        model.addAttribute("tipos", TipoMantenimiento.values());
        // Solo los talleres del propio usuario
        model.addAttribute("talleres", tallerService.listarDelUsuario(usuario.getId()));
    }
}
