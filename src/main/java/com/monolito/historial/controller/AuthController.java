package com.monolito.historial.controller;

import com.monolito.historial.dto.RegistroForm;
import com.monolito.historial.model.Usuario;
import com.monolito.historial.repository.UsuarioRepository;
import com.monolito.historial.service.EmailService;
import com.monolito.historial.service.TallerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TallerService tallerService;
    private final EmailService emailService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          TallerService tallerService,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tallerService = tallerService;
        this.emailService = emailService;
    }

    // GET /login  (el POST /login lo gestiona Spring Security)
    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        asegurarSesion(request);
        return "auth/login";
    }

    // GET /registro
    @GetMapping("/registro")
    public String formularioRegistro(Model model, HttpServletRequest request) {
        asegurarSesion(request);
        model.addAttribute("registro", new RegistroForm());
        return "auth/registro";
    }

    // POST /registro
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroForm registro,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {

        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmarPassword")
                && !registro.getPassword().equals(registro.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "password.noCoincide", "Las contraseñas no coinciden");
        }

        String email = registro.getEmail() == null ? "" : registro.getEmail().trim().toLowerCase();
        if (!result.hasFieldErrors("email") && usuarioRepository.existsByEmail(email)) {
            result.rejectValue("email", "email.duplicado", "Ya existe una cuenta con ese email");
        }

        if (result.hasErrors()) {
            return "auth/registro";
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(registro.getNombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(registro.getPassword()));
        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Alguien registró ese mismo email justo antes (restricción UNIQUE de la base de datos)
            result.rejectValue("email", "email.duplicado", "Ya existe una cuenta con ese email");
            return "auth/registro";
        }

        // Todo usuario nuevo empieza con un "Taller de confianza" para poder registrar visitas sin crear ninguno
        try {
            tallerService.crearConfianzaPorDefecto(usuario);
        } catch (RuntimeException e) {
            // No es grave: se volverá a crear automáticamente al iniciar sesión
            log.warn("No se pudo crear el taller de confianza inicial del usuario {}", usuario.getId(), e);
        }

        // Correo de bienvenida (en segundo plano: si falla, el registro ya está hecho)
        try {
            emailService.enviarBienvenida(usuario.getEmail(), usuario.getNombre());
        } catch (RuntimeException e) {
            log.warn("No se pudo programar el correo de bienvenida", e);
        }

        redirectAttributes.addFlashAttribute("registrado", true);
        redirectAttributes.addFlashAttribute("correoBienvenida", emailService.estaActivo());
        return "redirect:/login";
    }

    /**
     * El token CSRF de los formularios (login y registro) se guarda en la sesión. Un visitante que
     * llega por primera vez todavía no la tiene, y crearla exige enviar una cookie. Eso solo se puede
     * hacer antes de que empiece a salir la página; por eso se crea aquí, antes de pintar la plantilla.
     */
    private void asegurarSesion(HttpServletRequest request) {
        request.getSession(true);
    }
}