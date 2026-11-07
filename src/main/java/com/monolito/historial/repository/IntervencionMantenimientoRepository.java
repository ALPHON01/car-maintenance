package com.monolito.historial.repository;

import com.monolito.historial.model.IntervencionMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntervencionMantenimientoRepository extends JpaRepository<IntervencionMantenimiento, Long> {

    List<IntervencionMantenimiento> findByVehiculoIdOrderByFechaDesc(Long vehiculoId);

    // Una visita solo si pertenece a ese vehículo
    Optional<IntervencionMantenimiento> findByIdAndVehiculoId(Long id, Long vehiculoId);

    // Para no borrar talleres que ya tienen visitas registradas
    boolean existsByTallerId(Long tallerId);
}