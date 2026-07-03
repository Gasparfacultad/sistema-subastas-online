package com.utn.frvm.subastas.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialIncidenciaResponseDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioUsername;
    private Long disputaId;
    private String motivoPenalizacion;
    private LocalDateTime fechaRegistro;
}
