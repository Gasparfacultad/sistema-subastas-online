package com.utn.frvm.subastas.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PujaResponseDTO {

    private Long id;
    private Long subastaId;
    private Long compradorId;
    private String compradorUsername;
    private BigDecimal monto;
    private LocalDateTime fechaHora;
    private Boolean esGanadora;
}
