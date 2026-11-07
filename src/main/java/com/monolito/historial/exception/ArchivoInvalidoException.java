package com.monolito.historial.exception;

/** Un archivo adjunto no es válido (tipo, tamaño o número de archivos). El mensaje se puede mostrar al usuario. */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
