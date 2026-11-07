package com.monolito.historial.exception;

import com.monolito.historial.security.UsuarioPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Convierte los fallos de base de datos en una página de error amable
 * en vez de la pantalla técnica con el stack trace.
 */
@ControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Restricciones UNIQUE o claves foráneas (matrícula repetida, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView integridad(DataIntegrityViolationException ex, Authentication authentication) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return vista(HttpStatus.CONFLICT,
                "Esos datos ya existen",
                "Ya hay un registro con esos datos, por ejemplo una matrícula que ya está dada de alta.",
                authentication);
    }

    // Cualquier otro fallo de acceso a datos (incluye "No row with the given identifier exists...")
    @ExceptionHandler(DataAccessException.class)
    public ModelAndView datos(DataAccessException ex, Authentication authentication) {
        log.error("Error de acceso a datos", ex);
        return vista(HttpStatus.INTERNAL_SERVER_ERROR,
                "Problema con los datos",
                "No hemos podido completar la operación. Inténtalo de nuevo y, si sigue fallando, avisa al administrador.",
                authentication);
    }

    // Un archivo (o el envío completo) supera el límite configurado en application.properties
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView archivoDemasiadoGrande(MaxUploadSizeExceededException ex, Authentication authentication) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        return vista(HttpStatus.PAYLOAD_TOO_LARGE,
                "Archivo demasiado grande",
                "Cada archivo puede pesar como máximo 5 MB. Vuelve atrás y elige archivos más pequeños.",
                authentication);
    }

    private ModelAndView vista(HttpStatus status, String titulo, String mensaje, Authentication authentication) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("titulo", titulo);
        mav.addObject("mensaje", mensaje);
        // Los métodos @ExceptionHandler no pasan por GlobalModelAttributes: se añade aquí para el header
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            mav.addObject("usuarioActual", principal);
        }
        return mav;
    }
}
