package com.monolito.historial.exception;

/** Se lanza cuando un taller no se puede borrar (es el de confianza o tiene visitas). */
public class TallerNoEliminableException extends RuntimeException {

    public TallerNoEliminableException(String mensaje) {
        super(mensaje);
    }
}
