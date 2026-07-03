package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.TipoNotificacion;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionResponseDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioUsername;
    private Long subastaId;
    private String subastaTitulo;
    private TipoNotificacion tipo;
    private String mensaje;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
}
