package com.utn.frvm.subastas.entities;

import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "incidencias_acumuladas", nullable = false)
    @Builder.Default
    private Integer incidenciasAcumuladas = 0;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloqueado_por_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario bloqueadoPor;

    @Column(name = "motivo_bloqueo", length = 255)
    private String motivoBloqueo;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        if (this.incidenciasAcumuladas == null) {
            this.incidenciasAcumuladas = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
