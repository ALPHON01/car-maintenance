package com.monolito.historial.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide a dónde mandar al usuario cuando el login falla, según el tipo de excepción:
 *  - credenciales: email o contraseña incorrectos (no se revela cuál de los dos)
 *  - cuenta: cuenta deshabilitada, bloqueada o caducada
 *  - sistema: fallo inesperado (por ejemplo, la base de datos no responde)
 */
@Component
public class AuthFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String motivo;

        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            log.warn("Intento de login fallido: credenciales incorrectas");
            motivo = "credenciales";
        } else if (exception instanceof AccountStatusException) {
            log.warn("Intento de login rechazado: {}", exception.getClass().getSimpleName());
            motivo = "cuenta";
        } else {
            // Por ejemplo InternalAuthenticationServiceException: falló algo al buscar el usuario
            log.error("Error inesperado durante la autenticación", exception);
            motivo = "sistema";
        }

        response.sendRedirect(request.getContextPath() + "/login?error=" + motivo);
    }
}