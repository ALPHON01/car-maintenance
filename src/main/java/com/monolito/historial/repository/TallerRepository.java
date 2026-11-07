package com.monolito.historial.repository;

import com.monolito.historial.model.Taller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TallerRepository extends JpaRepository<Taller, Long> {

    // El taller de confianza sale primero y después el resto por nombre
    List<Taller> findByUsuarioIdOrderByConfianzaDescNombreAsc(Long usuarioId);

    // Un taller solo si pertenece a ese usuario
    Optional<Taller> findByIdAndUsuarioId(Long id, Long usuarioId);

    // El taller de confianza del usuario (First por si hubiera datos duplicados)
    Optional<Taller> findFirstByUsuarioIdAndConfianzaTrue(Long usuarioId);
}
