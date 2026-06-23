package com.utn.frvm.subastas.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pujas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Puja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subasta_id", nullable = false)
    private Subasta subasta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    private Usuario comprador;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "es_ganadora", nullable = false)
    @Builder.Default
    private Boolean esGanadora = false;

    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
        if (this.esGanadora == null) {
            this.esGanadora = false;
        }
    }
}
