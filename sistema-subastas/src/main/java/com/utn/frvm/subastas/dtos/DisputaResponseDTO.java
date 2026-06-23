package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoDisputa;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputaResponseDTO {

    private Long id;
    private Long subastaId;
    private Long iniciadorId;
    private String iniciadorUsername;
    private Long adminResolutorId;
    private String adminResolutorUsername;
    private String motivoApertura;
    private String justificacionResolucion;
    private EstadoDisputa estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaResolucion;
}
