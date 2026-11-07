package com.monolito.historial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Datos del formulario de talleres. No incluye propietario ni "confianza": eso lo decide el servidor. */
@Getter
@Setter
public class TallerForm {

    private Long id;

    @NotBlank(message = "El nombre del taller es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;
}
