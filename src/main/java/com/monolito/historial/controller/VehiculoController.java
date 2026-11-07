package com.monolito.historial.controller;

import com.monolito.historial.model.IntervencionMantenimiento;
import com.monolito.historial.model.TipoMantenimiento;
import com.monolito.historial.model.Vehiculo;
import com.monolito.historial.repository.UsuarioRepository;
import com.monolito.historial.repository.VehiculoRepository;
import com.monolito.historial.security.UsuarioPrincipal;
import com.monolito.historial.service.AdjuntoService;
import com.monolito.historial.service.MantenimientoService;
import com.monolito.historial.service.TallerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/vehiculos")
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TallerService tallerService;
    private final MantenimientoService mantenimientoService;
    private final AdjuntoService adjuntoService;

    public VehiculoController(VehiculoRepository vehiculoRepository,
                              UsuarioRepository usuarioRepository,
                              TallerService tallerService,
                              MantenimientoService mantenimientoService,
                              AdjuntoService adjuntoService) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tallerService = tallerService;
        this.mantenimientoService = mantenimientoService;
        this.adjuntoService = adjuntoService;
    }

    // El formulario solo puede rellenar los datos del coche: nunca el id,
    // el propietario ni las listas (evita que alguien manipule la petición).
    @InitBinder("vehiculo")
    public void configurarBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id", "usuario*", "intervenciones*", "pendientes*");
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        model.addAttribute("vehiculos", vehiculoRepository.findByUsuarioId(usuario.getId()));
        return "vehiculos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("vehiculo", new Vehiculo());
        return "vehiculos/formulario";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute Vehiculo vehiculo,
                          BindingResult result,
                          @AuthenticationPrincipal UsuarioPrincipal usuario) {
        if (result.hasErrors()) {
            return "vehiculos/formulario";
        }
        // El propietario siempre es quien tiene la sesión iniciada
        // Si el usuario de la sesión ya no existe en la base de datos, se corta aquí con un 401
        vehiculo.setUsuario(usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no es válida")));
        vehiculoRepository.save(vehiculo);
        return "redirect:/vehiculos";
    }

    @GetMapping("/{id:[0-9]+}")
    public String ficha(@PathVariable Long id,
                        @AuthenticationPrincipal UsuarioPrincipal usuario,
                        Model model) {
        // Si el vehículo existe pero es de otro usuario, responde 404 igual que si no existiera
        Vehiculo vehiculo = vehiculoRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        model.addAttribute("vehiculo", vehiculo);
        List<IntervencionMantenimiento> historial = mantenimientoService.obtenerHistorial(id);
        model.addAttribute("historial", historial);
        // Facturas adjuntas de cada visita (id de la visita -> lista de archivos)
        model.addAttribute("adjuntosPorIntervencion", adjuntoService.agruparPorIntervencion(historial));
        model.addAttribute("pendientes", mantenimientoService.obtenerPendientes(id));
        // Para los desplegables de los modales de la ficha
        model.addAttribute("talleres", tallerService.listarDelUsuario(usuario.getId()));
        model.addAttribute("tipos", TipoMantenimiento.values());
        return "vehiculos/ficha";
    }
}
