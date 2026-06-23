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

    @NotNull(message = "El vendedor es obligatorio")
    private Long vendedorId;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El precio base es obligatorio")
    @Positive(message = "El precio base debe ser mayor a cero")
    private BigDecimal precioBase;

    @NotNull(message = "El incremento mínimo de puja es obligatorio")
    @Positive(message = "El incremento mínimo de puja debe ser mayor a cero")
    private BigDecimal incrementoMinimoPuja;

    @NotBlank(message = "El título de la subasta no puede estar vacío")
    @Size(min = 5, max = 150, message = "El título debe tener entre 5 y 150 caracteres")
    private String titulo;

    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio debe ser en el presente o futuro")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    @Future(message = "La fecha de cierre debe ser en el futuro")
    private LocalDateTime fechaCierre;

    @NotNull(message = "El estado de la subasta es obligatorio")
    private EstadoSubasta estado;
}
