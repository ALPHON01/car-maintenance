package com.monolito.historial.repository;

import com.monolito.historial.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Usado por Spring Security al iniciar sesión (el email es el nombre de usuario)
    Optional<Usuario> findByEmail(String email);

    // Usado en el registro para evitar cuentas duplicadas
    boolean existsByEmail(String email);
}