package com.monolito.historial.controller;

import com.monolito.historial.dto.TallerForm;
import com.monolito.historial.exception.TallerNoEliminableException;
import com.monolito.historial.model.Taller;
import com.monolito.historial.security.SesionUsuario;
import com.monolito.historial.security.UsuarioPrincipal;
import com.monolito.historial.service.TallerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/talleres")
public class TallerController {

    private final TallerService tallerService;

    public TallerController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    // GET /talleres  (solo los talleres del usuario; el de confianza, el primero)
    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        model.addAttribute("talleres", tallerService.listarDelUsuario(usuario.getId()));
        return "talleres/lista";
    }

    // GET /talleres/nuevo
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("taller", new TallerForm());
        return "talleres/formulario";
    }

    // GET /talleres/{id}/editar
    @GetMapping("/{id:[0-9]+}/editar")
    public String formularioEditar(@PathVariable Long id,
                                   @AuthenticationPrincipal UsuarioPrincipal usuario,
                                   Model model) {
        Taller taller = tallerService.obtenerDelUsuario(id, usuario.getId());

        TallerForm form = new TallerForm();
        form.setId(taller.getId());
        form.setNombre(taller.getNombre());
        form.setTelefono(taller.getTelefono());
        form.setDireccion(taller.getDireccion());

        model.addAttribute("taller", form);
        return "talleres/formulario";
    }

    // POST /talleres  (crea si no hay id, actualiza si lo hay)
    @PostMapping
    public String guardar(@Valid @ModelAttribute("taller") TallerForm form,
                          BindingResult result,
                          @AuthenticationPrincipal UsuarioPrincipal usuario,
                          HttpSession sesion,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "talleres/formulario";
        }
        tallerService.guardar(form, usuario.getId());
        refrescarSesion(sesion, usuario);   // por si se ha renombrado el de confianza

        redirectAttributes.addFlashAttribute("mensaje", "Taller guardado correctamente.");
        return "redirect:/talleres";
    }

    // POST /talleres/{id}/confianza  (pasa la estrella a este taller)
    @PostMapping("/{id:[0-9]+}/confianza")
    public String marcarComoConfianza(@PathVariable Long id,
                                      @AuthenticationPrincipal UsuarioPrincipal usuario,
                                      HttpSession sesion,
                                      RedirectAttributes redirectAttributes) {
        Taller elegido = tallerService.marcarComoConfianza(id, usuario.getId());
        SesionUsuario.actualizarTallerConfianza(sesion, elegido);

        redirectAttributes.addFlashAttribute("mensaje", "Taller de confianza actualizado.");
        return "redirect:/talleres";
    }

    // POST /talleres/{id}/eliminar
    @PostMapping("/{id:[0-9]+}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @AuthenticationPrincipal UsuarioPrincipal usuario,
                           RedirectAttributes redirectAttributes) {
        try {
            tallerService.eliminar(id, usuario.getId());
            redirectAttributes.addFlashAttribute("mensaje", "Taller eliminado.");
        } catch (TallerNoEliminableException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/talleres";
    }

    // Mantiene en la sesión el nombre actual del taller de confianza
    private void refrescarSesion(HttpSession sesion, UsuarioPrincipal usuario) {
        SesionUsuario.actualizarTallerConfianza(sesion, tallerService.obtenerOCrearConfianza(usuario.getId()));
    }
}
