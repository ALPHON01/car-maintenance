package com.monolito.historial.util;

import org.springframework.validation.BindingResult;

import java.util.stream.Collectors;

public final class Formularios {

    private Formularios() {
    }

    /** Junta los errores de validación en una sola frase legible para mostrarla en un aviso. */
    public static String resumenErrores(BindingResult result) {
        return result.getFieldErrors().stream()
                .map(e -> e.isBindingFailure()
                        ? "El valor del campo \u00ab" + e.getField() + "\u00bb no es válido"
                        : e.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" \u00b7 "));
    }
}