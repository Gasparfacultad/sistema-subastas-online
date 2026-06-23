package com.utn.frvm.subastas.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historiales_incidencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disputa_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Disputa disputa;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "motivo_penalizacion", nullable = false, length = 255)
    private String motivoPenalizacion;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}
