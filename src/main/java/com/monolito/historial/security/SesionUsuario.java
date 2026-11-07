package com.monolito.historial.security;

import com.monolito.historial.model.Taller;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Variables de sesión de la aplicación.
 * Se crean al iniciar sesión (LoginSuccessHandler) y se eliminan al cerrarla (LogoutSesionHandler).
 * En las plantillas se leen como ${session.usuarioNombre}, ${session.tallerConfianzaNombre}, etc.
 */
public final class SesionUsuario {

    public static final String USUARIO_ID = "usuarioId";
    public static final String USUARIO_NOMBRE = "usuarioNombre";
    public static final String USUARIO_EMAIL = "usuarioEmail";
    public static final String INICIO_SESION = "inicioSesion";
    public static final String TALLER_CONFIANZA_ID = "tallerConfianzaId";
    public static final String TALLER_CONFIANZA_NOMBRE = "tallerConfianzaNombre";

    private static final List<String> CLAVES = List.of(
            USUARIO_ID, USUARIO_NOMBRE, USUARIO_EMAIL, INICIO_SESION,
            TALLER_CONFIANZA_ID, TALLER_CONFIANZA_NOMBRE);

    private SesionUsuario() {
    }

    /** Crea las variables de sesión al iniciar sesión. */
    public static void iniciar(HttpSession sesion, UsuarioPrincipal usuario, Taller tallerConfianza) {
        sesion.setAttribute(USUARIO_ID, usuario.getId());
        sesion.setAttribute(USUARIO_NOMBRE, usuario.getNombre());
        sesion.setAttribute(USUARIO_EMAIL, usuario.getUsername());
        sesion.setAttribute(INICIO_SESION, LocalDateTime.now());
        actualizarTallerConfianza(sesion, tallerConfianza);
    }

    /** Refresca el taller de confianza guardado en la sesión (al marcar otro o al renombrarlo). */
    public static void actualizarTallerConfianza(HttpSession sesion, Taller taller) {
        if (taller == null) {
            sesion.removeAttribute(TALLER_CONFIANZA_ID);
            sesion.removeAttribute(TALLER_CONFIANZA_NOMBRE);
            return;
        }
        sesion.setAttribute(TALLER_CONFIANZA_ID, taller.getId());
        sesion.setAttribute(TALLER_CONFIANZA_NOMBRE, taller.getNombre());
    }

    /** Elimina todas las variables de sesión de la aplicación. */
    public static void limpiar(HttpSession sesion) {
        CLAVES.forEach(sesion::removeAttribute);
    }
}
