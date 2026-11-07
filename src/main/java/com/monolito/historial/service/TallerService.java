package com.monolito.historial.service;

import com.monolito.historial.dto.TallerForm;
import com.monolito.historial.exception.TallerNoEliminableException;
import com.monolito.historial.model.Taller;
import com.monolito.historial.model.Usuario;
import com.monolito.historial.repository.IntervencionMantenimientoRepository;
import com.monolito.historial.repository.TallerRepository;
import com.monolito.historial.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Talleres de cada usuario. Cada usuario solo ve y gestiona los suyos y siempre tiene
 * exactamente un "taller de confianza", al que se asignan las visitas cuando no elige otro.
 */
@Service
public class TallerService {

    public static final String NOMBRE_TALLER_INICIAL = "Taller de confianza";

    private final TallerRepository tallerRepository;
    private final UsuarioRepository usuarioRepository;
    private final IntervencionMantenimientoRepository intervencionRepository;

    public TallerService(TallerRepository tallerRepository,
                         UsuarioRepository usuarioRepository,
                         IntervencionMantenimientoRepository intervencionRepository) {
        this.tallerRepository = tallerRepository;
        this.usuarioRepository = usuarioRepository;
        this.intervencionRepository = intervencionRepository;
    }

    // ------------------------------------------------------------------
    // Taller de confianza
    // ------------------------------------------------------------------

    /** Devuelve el taller de confianza del usuario y, si aún no tiene, lo crea. */
    @Transactional
    public Taller obtenerOCrearConfianza(Long usuarioId) {
        return tallerRepository.findFirstByUsuarioIdAndConfianzaTrue(usuarioId)
                .orElseGet(() -> crearConfianza(usuarioRepository.getReferenceById(usuarioId)));
    }

    /** Se llama al registrarse: todo usuario nuevo empieza con su "Taller de confianza". */
    @Transactional
    public Taller crearConfianzaPorDefecto(Usuario usuario) {
        return tallerRepository.findFirstByUsuarioIdAndConfianzaTrue(usuario.getId())
                .orElseGet(() -> crearConfianza(usuario));
    }

    private Taller crearConfianza(Usuario propietario) {
        Taller taller = new Taller();
        taller.setNombre(NOMBRE_TALLER_INICIAL);
        taller.setConfianza(true);
        taller.setUsuario(propietario);
        return tallerRepository.save(taller);
    }

    /** Pasa la estrella a este taller. El anterior taller de confianza deja de serlo. */
    @Transactional
    public Taller marcarComoConfianza(Long tallerId, Long usuarioId) {
        List<Taller> talleres = tallerRepository.findByUsuarioIdOrderByConfianzaDescNombreAsc(usuarioId);

        Taller elegido = talleres.stream()
                .filter(t -> t.getId().equals(tallerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado"));

        talleres.forEach(t -> t.setConfianza(t.getId().equals(tallerId)));
        tallerRepository.saveAll(talleres);
        return elegido;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Talleres del usuario con el de confianza primero (y creándolo si todavía no existe). */
    @Transactional
    public List<Taller> listarDelUsuario(Long usuarioId) {
        obtenerOCrearConfianza(usuarioId);
        return tallerRepository.findByUsuarioIdOrderByConfianzaDescNombreAsc(usuarioId);
    }

    /** Devuelve el taller solo si es del usuario; si no, responde 404. */
    public Taller obtenerDelUsuario(Long tallerId, Long usuarioId) {
        return tallerRepository.findByIdAndUsuarioId(tallerId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado"));
    }

    // ------------------------------------------------------------------
    // Alta, edición y baja
    // ------------------------------------------------------------------

    /** Crea un taller nuevo (formulario sin id) o actualiza uno propio (con id). */
    @Transactional
    public Taller guardar(TallerForm form, Long usuarioId) {
        String nombre = form.getNombre().trim();
        String telefono = limpiar(form.getTelefono());
        String direccion = limpiar(form.getDireccion());

        if (form.getId() == null) {
            // Antes de añadir el primero propio, se asegura que exista el de confianza
            obtenerOCrearConfianza(usuarioId);

            Taller nuevo = new Taller();
            nuevo.setNombre(nombre);
            nuevo.setTelefono(telefono);
            nuevo.setDireccion(direccion);
            nuevo.setConfianza(false);
            nuevo.setUsuario(usuarioRepository.getReferenceById(usuarioId));
            return tallerRepository.save(nuevo);
        }

        Taller existente = obtenerDelUsuario(form.getId(), usuarioId);
        existente.setNombre(nombre);
        existente.setTelefono(telefono);
        existente.setDireccion(direccion);
        return tallerRepository.save(existente);
    }

    /** Elimina un taller propio. No se puede borrar el de confianza ni uno con visitas. */
    @Transactional
    public void eliminar(Long tallerId, Long usuarioId) {
        Taller taller = obtenerDelUsuario(tallerId, usuarioId);

        if (taller.isConfianza()) {
            throw new TallerNoEliminableException(
                    "No puedes eliminar tu taller de confianza. Marca otro como de confianza antes.");
        }
        if (intervencionRepository.existsByTallerId(tallerId)) {
            throw new TallerNoEliminableException("No se puede eliminar: el taller tiene visitas registradas.");
        }
        tallerRepository.delete(taller);
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
