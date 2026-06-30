package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoSubasta;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstadoResponseDTO {

    private Long id;
    private Long subastaId;
    private String subastaTitulo;
    private Long usuarioResponsableId;
    private String usuarioResponsableUsername;
    private EstadoSubasta estadoAnterior;
    private EstadoSubasta estadoNuevo;
    private String motivo;
    private LocalDateTime fechaCambio;
}
