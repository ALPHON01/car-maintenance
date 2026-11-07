package com.monolito.historial.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "talleres")
@Getter
@Setter
@NoArgsConstructor
public class Taller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del taller es obligatorio")
    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String direccion;

    // Cada usuario tiene exactamente un taller de confianza (se propone por defecto en las visitas)
    @Column(nullable = false)
    private boolean confianza = false;

    // Propietario: cada usuario solo ve y gestiona sus talleres
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
