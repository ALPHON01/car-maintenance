package com.monolito.historial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bytes del archivo adjunto. Van en una tabla aparte para que al listar los adjuntos
 * no se cargue el contenido de todos los archivos en memoria.
 */
@Entity
@Table(name = "adjuntos_contenido")
@Getter
@Setter
@NoArgsConstructor
public class AdjuntoContenido {

    // Mismo id que el adjunto al que pertenece
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private Adjunto adjunto;

    // Sin "length", Hibernate crea en MySQL una columna diminuta (TINYBLOB, 255 bytes) y casi
    // ningún archivo cabe. Con la longitud máxima pide LONGBLOB.
    @Lob
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private byte[] datos;
}