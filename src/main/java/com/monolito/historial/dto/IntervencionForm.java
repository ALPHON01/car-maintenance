package com.monolito.historial.dto;

import com.monolito.historial.model.TipoMantenimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que se envían desde los modales de la ficha del vehículo:
 * "marcar como realizado" y "editar visita".
 */
@Getter
@Setter
public class IntervencionForm {

    // Al editar una visita es obligatorio; al completar un pendiente lo fija el propio pendiente
    private TipoMantenimiento tipo;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @PositiveOrZero(message = "El kilometraje no puede ser negativo")
    private Integer kilometraje;

    @PositiveOrZero(message = "El coste no puede ser negativo")
    private BigDecimal coste;

    private Long tallerId;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String descripcion;
}