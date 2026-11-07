package com.monolito.historial.service;

import com.monolito.historial.dto.ArchivoSubido;
import com.monolito.historial.dto.IntervencionForm;
import com.monolito.historial.model.*;
import com.monolito.historial.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class MantenimientoService {

    private final VehiculoRepository vehiculoRepository;
    private final IntervencionMantenimientoRepository intervencionRepository;
    private final MantenimientoPendienteRepository pendienteRepository;
    private final TallerRepository tallerRepository;
    private final TallerService tallerService;
    private final AdjuntoService adjuntoService;

    // Intervalos por defecto: cada cuántos km y cada cuántos meses toca revisar
    private static final Map<TipoMantenimiento, Integer> INTERVALO_KM = Map.of(
            TipoMantenimiento.CAMBIO_ACEITE, 10000,
            TipoMantenimiento.FRENOS, 30000,
            TipoMantenimiento.NEUMATICOS, 40000,
            TipoMantenimiento.CORREA_DISTRIBUCION, 90000
    );

    private static final Map<TipoMantenimiento, Integer> INTERVALO_MESES = Map.of(
            TipoMantenimiento.ITV, 12,
            TipoMantenimiento.REVISION_GENERAL, 12
    );

    public MantenimientoService(VehiculoRepository vehiculoRepository,
                                IntervencionMantenimientoRepository intervencionRepository,
                                MantenimientoPendienteRepository pendienteRepository,
                                TallerRepository tallerRepository,
                                TallerService tallerService,
                                AdjuntoService adjuntoService) {
        this.vehiculoRepository = vehiculoRepository;
        this.intervencionRepository = intervencionRepository;
        this.pendienteRepository = pendienteRepository;
        this.tallerRepository = tallerRepository;
        this.tallerService = tallerService;
        this.adjuntoService = adjuntoService;
    }

    // ------------------------------------------------------------------
    // Alta de visitas
    // ------------------------------------------------------------------

    /** Registra una visita nueva a partir del formulario. Sin taller elegido, usa el de confianza. */
    @Transactional
    public IntervencionMantenimiento registrarIntervencion(Vehiculo vehiculo, IntervencionForm form) {
        IntervencionMantenimiento intervencion = new IntervencionMantenimiento();
        intervencion.setVehiculo(vehiculo);
        intervencion.setTipo(form.getTipo());
        aplicarDatos(intervencion, form);
        return registrarIntervencion(intervencion);
    }

    @Transactional
    public IntervencionMantenimiento registrarIntervencion(IntervencionMantenimiento intervencion) {
        actualizarKilometraje(intervencion.getVehiculo(), intervencion.getKilometrajeEnIntervencion());

        IntervencionMantenimiento guardada = intervencionRepository.save(intervencion);
        generarProximoPendienteSiAplica(guardada);
        return guardada;
    }

    /**
     * Marca un mantenimiento pendiente como realizado: crea la visita en el historial
     * con los datos confirmados por el usuario, cierra el pendiente y,
     * como en cualquier visita nueva, calcula el siguiente mantenimiento.
     */
    @Transactional
    public IntervencionMantenimiento completarPendiente(Vehiculo vehiculo, Long pendienteId, IntervencionForm form) {
        MantenimientoPendiente pendiente = pendienteRepository.findByIdAndVehiculoId(pendienteId, vehiculo.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado"));

        if (pendiente.getEstado() == MantenimientoPendiente.EstadoPendiente.COMPLETADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este mantenimiento ya estaba marcado como realizado");
        }

        IntervencionMantenimiento intervencion = new IntervencionMantenimiento();
        intervencion.setVehiculo(vehiculo);
        intervencion.setTipo(pendiente.getTipo()); // el tipo lo fija el pendiente
        aplicarDatos(intervencion, form);

        IntervencionMantenimiento guardada = registrarIntervencion(intervencion);

        pendiente.setEstado(MantenimientoPendiente.EstadoPendiente.COMPLETADO);
        pendienteRepository.save(pendiente);
        return guardada;
    }

    // ------------------------------------------------------------------
    // Edición de visitas
    // ------------------------------------------------------------------

    /** Modifica una visita existente. No recalcula los pendientes para no duplicarlos. */
    @Transactional
    public IntervencionMantenimiento actualizarIntervencion(Vehiculo vehiculo, Long intervencionId, IntervencionForm form) {
        IntervencionMantenimiento intervencion = intervencionRepository
                .findByIdAndVehiculoId(intervencionId, vehiculo.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));

        intervencion.setTipo(form.getTipo());
        aplicarDatos(intervencion, form);
        actualizarKilometraje(vehiculo, form.getKilometraje());

        return intervencionRepository.save(intervencion);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<MantenimientoPendiente> obtenerPendientes(Long vehiculoId) {
        return pendienteRepository.findByVehiculoIdAndEstado(
                vehiculoId, MantenimientoPendiente.EstadoPendiente.PENDIENTE);
    }

    public List<IntervencionMantenimiento> obtenerHistorial(Long vehiculoId) {
        return intervencionRepository.findByVehiculoIdOrderByFechaDesc(vehiculoId);
    }

    // ------------------------------------------------------------------
    // Con facturas adjuntas (PDF o fotos): visita y archivos se guardan en una sola transacción;
    // si algún archivo no es válido, no se guarda nada.
    // ------------------------------------------------------------------

    @Transactional
    public IntervencionMantenimiento registrarIntervencion(Vehiculo vehiculo, IntervencionForm form,
                                                           List<ArchivoSubido> archivos) {
        IntervencionMantenimiento guardada = registrarIntervencion(vehiculo, form);
        adjuntoService.adjuntar(guardada, archivos);
        return guardada;
    }

    @Transactional
    public IntervencionMantenimiento completarPendiente(Vehiculo vehiculo, Long pendienteId,
                                                        IntervencionForm form, List<ArchivoSubido> archivos) {
        IntervencionMantenimiento guardada = completarPendiente(vehiculo, pendienteId, form);
        adjuntoService.adjuntar(guardada, archivos);
        return guardada;
    }

    @Transactional
    public IntervencionMantenimiento actualizarIntervencion(Vehiculo vehiculo, Long intervencionId,
                                                            IntervencionForm form, List<ArchivoSubido> archivos) {
        IntervencionMantenimiento guardada = actualizarIntervencion(vehiculo, intervencionId, form);
        adjuntoService.adjuntar(guardada, archivos);
        return guardada;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void aplicarDatos(IntervencionMantenimiento intervencion, IntervencionForm form) {
        intervencion.setFecha(form.getFecha());
        intervencion.setKilometrajeEnIntervencion(form.getKilometraje());
        intervencion.setCoste(form.getCoste());

        String descripcion = form.getDescripcion();
        intervencion.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());

        intervencion.setTaller(buscarTaller(form.getTallerId(), intervencion.getVehiculo().getUsuario()));
    }

    private Taller buscarTaller(Long tallerId, Usuario propietario) {
        if (tallerId == null) {
            // Sin taller elegido: se asigna el taller de confianza del usuario
            return tallerService.obtenerOCrearConfianza(propietario.getId());
        }
        // Solo vale un taller del propio usuario
        return tallerRepository.findByIdAndUsuarioId(tallerId, propietario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El taller seleccionado no existe"));
    }

    // El kilometraje del coche solo sube, nunca baja
    private void actualizarKilometraje(Vehiculo vehiculo, Integer km) {
        if (km != null && (vehiculo.getKilometrajeActual() == null || km > vehiculo.getKilometrajeActual())) {
            vehiculo.setKilometrajeActual(km);
            vehiculoRepository.save(vehiculo);
        }
    }

    private void generarProximoPendienteSiAplica(IntervencionMantenimiento intervencion) {
        TipoMantenimiento tipo = intervencion.getTipo();
        MantenimientoPendiente pendiente = new MantenimientoPendiente();
        pendiente.setVehiculo(intervencion.getVehiculo());
        pendiente.setTipo(tipo);

        boolean generar = false;

        if (INTERVALO_KM.containsKey(tipo) && intervencion.getKilometrajeEnIntervencion() != null) {
            pendiente.setKilometrajeEstimado(
                    intervencion.getKilometrajeEnIntervencion() + INTERVALO_KM.get(tipo));
            generar = true;
        }

        if (INTERVALO_MESES.containsKey(tipo)) {
            pendiente.setFechaEstimada(intervencion.getFecha().plusMonths(INTERVALO_MESES.get(tipo)));
            generar = true;
        }

        if (generar) {
            pendienteRepository.save(pendiente);
        }
    }
}
