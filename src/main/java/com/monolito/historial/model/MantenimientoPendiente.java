package com.monolito.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "mantenimientos_pendientes")
@Getter
@Setter
@NoArgsConstructor
public class MantenimientoPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMantenimiento tipo;

    @Column(name = "fecha_estimada")
    private LocalDate fechaEstimada;

    @Column(name = "kilometraje_estimado")
    private Integer kilometrajeEstimado;

    private EstadoPendiente estado = EstadoPendiente.PENDIENTE;

    public enum EstadoPendiente {
        PENDIENTE, AVISADO, COMPLETADO
    }

}
