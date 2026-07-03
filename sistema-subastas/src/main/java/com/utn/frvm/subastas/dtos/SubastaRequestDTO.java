package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoSubasta;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubastaRequestDTO {

    @NotNull(message = "{subasta.vendedorId.notnull}")
    private Long vendedorId;

    @NotNull(message = "{subasta.productoId.notnull}")
    private Long productoId;

    @NotNull(message = "{subasta.precioBase.notnull}")
    @Positive(message = "{subasta.precioBase.positive}")
    private BigDecimal precioBase;

    @NotNull(message = "{subasta.incrementoMinimoPuja.notnull}")
    @Positive(message = "{subasta.incrementoMinimoPuja.positive}")
    private BigDecimal incrementoMinimoPuja;

    @NotBlank(message = "{subasta.titulo.notblank}")
    @Size(min = 5, max = 150, message = "{subasta.titulo.size}")
    private String titulo;

    private String descripcion;

    @NotNull(message = "{subasta.fechaInicio.notnull}")
    @FutureOrPresent(message = "{subasta.fechaInicio.futureOrPresent}")
    private LocalDateTime fechaInicio;

    @NotNull(message = "{subasta.fechaCierre.notnull}")
    @Future(message = "{subasta.fechaCierre.future}")
    private LocalDateTime fechaCierre;

    @NotNull(message = "{subasta.estado.notnull}")
    private EstadoSubasta estado;
}
