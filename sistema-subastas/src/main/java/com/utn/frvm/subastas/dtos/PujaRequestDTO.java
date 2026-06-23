package com.utn.frvm.subastas.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaRequestDTO {

    @NotNull(message = "El ID de la subasta es obligatorio")
    private Long subastaId;

    @NotNull(message = "El ID del comprador es obligatorio")
    private Long compradorId;

    @NotNull(message = "El monto de la puja es obligatorio")
    @Positive(message = "El monto de la puja debe ser mayor a cero")
    private BigDecimal monto;
}
