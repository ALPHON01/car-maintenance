package com.monolito.historial.service;

import com.monolito.historial.dto.ArchivoSubido;
import com.monolito.historial.exception.ArchivoInvalidoException;
import com.monolito.historial.model.Adjunto;
import com.monolito.historial.model.AdjuntoContenido;
import com.monolito.historial.model.IntervencionMantenimiento;
import com.monolito.historial.repository.AdjuntoContenidoRepository;
import com.monolito.historial.repository.AdjuntoRepository;
import com.monolito.historial.repository.IntervencionMantenimientoRepository;
import com.monolito.historial.repository.VehiculoRepository;
import com.monolito.historial.util.TiposArchivo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Facturas (PDF o fotos) adjuntas a las visitas. Se guardan en la base de datos. */
@Service
public class AdjuntoService {

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    public static final int MAX_ARCHIVOS_POR_VISITA = 5;

    private final AdjuntoRepository adjuntoRepository;
    private final AdjuntoContenidoRepository contenidoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final IntervencionMantenimientoRepository intervencionRepository;

    public AdjuntoService(AdjuntoRepository adjuntoRepository,
                          AdjuntoContenidoRepository contenidoRepository,
                          VehiculoRepository vehiculoRepository,
                          IntervencionMantenimientoRepository intervencionRepository) {
        this.adjuntoRepository = adjuntoRepository;
        this.contenidoRepository = contenidoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.intervencionRepository = intervencionRepository;
    }

    /**
     * Valida los archivos recibidos del formulario (tipo real, tamaño y cantidad) y los deja listos.
     * Se llama ANTES de guardar nada, para no dejar una visita a medias si un archivo no vale.
     */
    public List<ArchivoSubido> preparar(List<MultipartFile> archivos) {
        List<ArchivoSubido> listos = new ArrayList<>();
        if (archivos == null) {
            return listos;
        }

        for (MultipartFile archivo : archivos) {
            // Si no se elige nada, el navegador envía igualmente una parte vacía
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }

            String nombre = TiposArchivo.limpiarNombre(archivo.getOriginalFilename());

            if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
                throw new ArchivoInvalidoException("«" + nombre + "» supera el máximo de 5 MB.");
            }

            byte[] datos;
            try {
                datos = archivo.getBytes();
            } catch (IOException e) {
                throw new ArchivoInvalidoException("No se ha podido leer «" + nombre + "». Inténtalo de nuevo.");
            }

            String tipo = TiposArchivo.detectar(datos);
            if (tipo == null) {
                throw new ArchivoInvalidoException("«" + nombre + "» no es un PDF ni una imagen JPG, PNG o WebP.");
            }
            listos.add(new ArchivoSubido(nombre, tipo, datos));
        }

        if (listos.size() > MAX_ARCHIVOS_POR_VISITA) {
            throw new ArchivoInvalidoException(
                    "Puedes adjuntar como máximo " + MAX_ARCHIVOS_POR_VISITA + " archivos por visita.");
        }
        return listos;
    }

    /** Guarda los archivos en la visita. Si se pasa del máximo por visita, lanza la excepción y se deshace todo. */
    @Transactional
    public void adjuntar(IntervencionMantenimiento intervencion, List<ArchivoSubido> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        long existentes = adjuntoRepository.countByIntervencionId(intervencion.getId());
        if (existentes + archivos.size() > MAX_ARCHIVOS_POR_VISITA) {
            throw new ArchivoInvalidoException("Cada visita admite como máximo " + MAX_ARCHIVOS_POR_VISITA
                    + " archivos y esta ya tiene " + existentes + ".");
        }

        for (ArchivoSubido archivo : archivos) {
            Adjunto adjunto = new Adjunto();
            adjunto.setIntervencion(intervencion);
            adjunto.setNombreOriginal(archivo.nombre());
            adjunto.setTipoContenido(archivo.tipoContenido());
            adjunto.setTamano(archivo.datos().length);
            adjunto = adjuntoRepository.save(adjunto);

            AdjuntoContenido contenido = new AdjuntoContenido();
            contenido.setAdjunto(adjunto);
            contenido.setDatos(archivo.datos());
            contenidoRepository.save(contenido);
        }
    }

    /** Adjuntos de todas las visitas indicadas, agrupados por id de visita (una sola consulta). */
    @Transactional(readOnly = true)
    public Map<Long, List<Adjunto>> agruparPorIntervencion(List<IntervencionMantenimiento> intervenciones) {
        if (intervenciones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = intervenciones.stream().map(IntervencionMantenimiento::getId).toList();
        return adjuntoRepository.findByIntervencionIdInOrderByFechaSubidaAsc(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getIntervencion().getId()));
    }

    /**
     * Devuelve el adjunto solo si la cadena usuario → vehículo → visita → adjunto es correcta.
     * En cualquier otro caso responde 404, como si no existiera.
     */
    @Transactional(readOnly = true)
    public Adjunto obtenerDelUsuario(Long usuarioId, Long vehiculoId, Long intervencionId, Long adjuntoId) {
        vehiculoRepository.findByIdAndUsuarioId(vehiculoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
        intervencionRepository.findByIdAndVehiculoId(intervencionId, vehiculoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));
        return adjuntoRepository.findByIdAndIntervencionId(adjuntoId, intervencionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
    }

    @Transactional(readOnly = true)
    public byte[] obtenerDatos(Adjunto adjunto) {
        return contenidoRepository.findById(adjunto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"))
                .getDatos();
    }

    @Transactional
    public void eliminar(Adjunto adjunto) {
        // Primero el contenido, que depende del adjunto
        contenidoRepository.deleteById(adjunto.getId());
        contenidoRepository.flush();
        adjuntoRepository.delete(adjunto);
    }
}
