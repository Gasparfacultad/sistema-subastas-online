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

    @NotNull(message = "{puja.subastaId.notnull}")
    private Long subastaId;

    @NotNull(message = "{puja.compradorId.notnull}")
    private Long compradorId;

    @NotNull(message = "{puja.monto.notnull}")
    @Positive(message = "{puja.monto.positive}")
    private BigDecimal monto;
}
