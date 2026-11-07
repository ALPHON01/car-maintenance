package com.monolito.historial.repository;

import com.monolito.historial.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    long countByIntervencionId(Long intervencionId);

    // Un adjunto solo si pertenece a esa visita
    Optional<Adjunto> findByIdAndIntervencionId(Long id, Long intervencionId);

    // Todos los adjuntos de varias visitas de una sola consulta
    List<Adjunto> findByIntervencionIdInOrderByFechaSubidaAsc(Collection<Long> intervencionIds);
}
