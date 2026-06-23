package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoProducto;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponseDTO {

    private Long id;
    private Long categoriaId;
    private String categoriaNombre;
    private Long vendedorId;
    private String vendedorUsername;
    private String nombre;
    private String descripcion;
    private EstadoProducto estado;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
