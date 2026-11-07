package com.monolito.historial.security;

import com.monolito.historial.model.Taller;
import com.monolito.historial.service.TallerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Se ejecuta al iniciar sesión correctamente: crea las variables de sesión y redirige a /vehiculos. */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginSuccessHandler.class);

    private final TallerService tallerService;

    public LoginSuccessHandler(TallerService tallerService) {
        this.tallerService = tallerService;
        setDefaultTargetUrl("/vehiculos");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof UsuarioPrincipal usuario) {
            Taller tallerConfianza = null;
            try {
                // Garantiza que el usuario tenga taller de confianza (también las cuentas anteriores)
                tallerConfianza = tallerService.obtenerOCrearConfianza(usuario.getId());
            } catch (RuntimeException e) {
                // El login no debe fallar por esto: se entra igualmente, sin ese dato en la sesión
                log.error("No se pudo obtener el taller de confianza del usuario {}", usuario.getId(), e);
            }
            SesionUsuario.iniciar(request.getSession(), usuario, tallerConfianza);
            log.info("Sesión iniciada para el usuario {}", usuario.getId());
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
