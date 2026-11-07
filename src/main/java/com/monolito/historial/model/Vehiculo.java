package com.monolito.historial.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La marca es obligatoria")
    @Column(nullable = false)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(nullable = false)
    private String modelo;

    @NotBlank(message = "La matrícula es obligatoria")
    @Column(nullable = false, unique = true)
    private String matricula;

    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(name = "kilometraje_actual")
    private Integer kilometrajeActual;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    // Propietario del vehículo
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntervencionMantenimiento> intervenciones = new ArrayList<>();

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MantenimientoPendiente> pendientes = new ArrayList<>();
}