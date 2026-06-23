package com.utn.frvm.subastas.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputaRequestDTO {

    @NotNull(message = "El ID de la subasta es obligatorio")
    private Long subastaId;

    @NotNull(message = "El ID del iniciador de la disputa es obligatorio")
    private Long iniciadorId;

    @NotBlank(message = "El motivo de apertura no puede estar vacío")
    private String motivoApertura;
}
