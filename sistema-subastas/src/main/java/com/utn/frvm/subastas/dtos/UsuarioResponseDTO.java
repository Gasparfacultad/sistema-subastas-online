package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String username;
    private String email;
    private RolUsuario rol;
    private EstadoUsuario estado;
    private String verificationToken;
    private Integer incidenciasAcumuladas;
    private LocalDateTime fechaRegistro;
    private Long bloqueadoPorId;
    private String motivoBloqueo;
    private LocalDateTime fechaBloqueo;
}
