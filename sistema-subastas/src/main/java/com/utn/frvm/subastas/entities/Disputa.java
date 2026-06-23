package com.utn.frvm.subastas.entities;

import com.utn.frvm.subastas.enums.EstadoDisputa;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subasta_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Subasta subasta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iniciador_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario iniciador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_resolutor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario adminResolutor;

    @Column(name = "motivo_apertura", columnDefinition = "TEXT", nullable = false)
    private String motivoApertura;

    @Column(name = "justificacion_resolucion", columnDefinition = "TEXT")
    private String justificacionResolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDisputa estado;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @PrePersist
    protected void onCreate() {
        this.fechaApertura = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoDisputa.ABIERTA;
        }
    }
}
