package com.monolito.historial.repository;

import com.monolito.historial.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByMatricula(String matricula);

    // Listado de los vehículos de un usuario
    List<Vehiculo> findByUsuarioId(Long usuarioId);

    // Un vehículo solo si pertenece a ese usuario
    Optional<Vehiculo> findByIdAndUsuarioId(Long id, Long usuarioId);
}