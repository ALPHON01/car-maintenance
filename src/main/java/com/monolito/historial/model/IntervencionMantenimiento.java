package com.monolito.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "intervenciones_mantenimiento")
@Getter
@Setter
@NoArgsConstructor
public class IntervencionMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @ManyToOne
    @JoinColumn(name = "taller_id")
    private Taller taller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMantenimiento tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "kilometraje_en_intervencion")
    private Integer kilometrajeEnIntervencion;

    private BigDecimal coste;

    @Column(length = 1000)
    private String descripcion;

}
