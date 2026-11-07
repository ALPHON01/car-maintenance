package com.monolito.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // Se usa como nombre de usuario para iniciar sesión (siempre en minúsculas)
    @Column(nullable = false, unique = true)
    private String email;

    // Hash BCrypt. Nunca se guarda la contraseña en claro
    @Column(nullable = false)
    private String password;
}