package com.utn.frvm.subastas.entities;

import com.utn.frvm.subastas.enums.EstadoSubasta;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subastas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private Usuario ganador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_actual_id")
    private Usuario ganadorActual;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "precio_final", precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "fecha_adjudicacion")
    private LocalDateTime fechaAdjudicacion;

    @Column(name = "incremento_minimo_puja", nullable = false, precision = 12, scale = 2)
    private BigDecimal incrementoMinimoPuja;

    @Column(name = "monto_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoActual;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSubasta estado;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.montoActual == null) {
            this.montoActual = this.precioBase;
        }
    }
}
