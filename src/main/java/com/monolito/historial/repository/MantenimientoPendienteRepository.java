package com.monolito.historial.repository;

import com.monolito.historial.model.MantenimientoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MantenimientoPendienteRepository extends JpaRepository<MantenimientoPendiente, Long> {

    List<MantenimientoPendiente> findByVehiculoIdAndEstado(
            Long vehiculoId, MantenimientoPendiente.EstadoPendiente estado);

    // Un pendiente solo si pertenece a ese vehículo
    Optional<MantenimientoPendiente> findByIdAndVehiculoId(Long id, Long vehiculoId);
}