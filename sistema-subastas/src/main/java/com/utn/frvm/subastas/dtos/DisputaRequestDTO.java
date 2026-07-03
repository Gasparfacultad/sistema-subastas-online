package com.utn.frvm.subastas.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputaRequestDTO {

    @NotNull(message = "{disputa.subastaId.notnull}")
    private Long subastaId;

    @NotNull(message = "{disputa.iniciadorId.notnull}")
    private Long iniciadorId;

    @NotBlank(message = "{disputa.motivoApertura.notblank}")
    private String motivoApertura;
}
