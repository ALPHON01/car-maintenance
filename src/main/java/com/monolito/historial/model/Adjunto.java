package com.monolito.historial.model;

import com.monolito.historial.util.TiposArchivo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

/** Datos de un archivo adjunto (factura) a una visita. El contenido va aparte, en AdjuntoContenido. */
@Entity
@Table(name = "adjuntos")
@Getter
@Setter
@NoArgsConstructor
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "intervencion_id")
    private IntervencionMantenimiento intervencion;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

    @Column(nullable = false)
    private long tamano;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida = LocalDateTime.now();

    public boolean isPdf() {
        return TiposArchivo.PDF.equals(tipoContenido);
    }

    /** Tamaño para mostrar: "480 KB", "1,2 MB"... */
    public String getTamanoLegible() {
        if (tamano < 1024) {
            return tamano + " B";
        }
        if (tamano < 1024 * 1024) {
            return Math.round(tamano / 1024.0) + " KB";
        }
        return String.format(Locale.of("es", "ES"), "%.1f MB", tamano / (1024.0 * 1024.0));
    }
}
