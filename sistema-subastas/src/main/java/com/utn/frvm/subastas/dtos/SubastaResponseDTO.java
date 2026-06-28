package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoSubasta;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubastaResponseDTO {

    private Long id;
    private Long vendedorId;
    private String vendedorUsername;
    private Long productoId;
    private String productoNombre;
    private Long ganadorId;
    private String ganadorUsername;
    private Long ganadorActualId;
    private String ganadorActualUsername;
    private BigDecimal precioBase;
    private BigDecimal precioFinal;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaAdjudicacion;
    private BigDecimal incrementoMinimoPuja;
    private BigDecimal montoActual;
    private String titulo;
    private String descripcion;
    private EstadoSubasta estado;
    private Long cantidadPujas;
}
