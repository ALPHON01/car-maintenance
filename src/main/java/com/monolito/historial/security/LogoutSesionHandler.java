package com.monolito.historial.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Se ejecuta al cerrar sesión: elimina las variables de sesión antes de invalidar la sesión. */
@Component
public class LogoutSesionHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(LogoutSesionHandler.class);

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            SesionUsuario.limpiar(sesion);
            log.info("Sesión cerrada: variables de sesión eliminadas");
        }
    }
}
