package com.monolito.historial.dto;

/** Archivo ya validado y listo para guardarse como adjunto. */
public record ArchivoSubido(String nombre, String tipoContenido, byte[] datos) {
}
