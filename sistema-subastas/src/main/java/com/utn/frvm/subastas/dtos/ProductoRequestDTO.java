package com.utn.frvm.subastas.dtos;

import com.utn.frvm.subastas.enums.EstadoProducto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRequestDTO {

    @NotNull(message = "{producto.categoriaId.notnull}")
    private Long categoriaId;

    @NotNull(message = "{producto.vendedorId.notnull}")
    private Long vendedorId;

    @NotBlank(message = "{producto.nombre.notblank}")
    @Size(min = 3, max = 150, message = "{producto.nombre.size}")
    private String nombre;

    @NotBlank(message = "{producto.descripcion.notblank}")
    private String descripcion;

    @NotNull(message = "{producto.estado.notnull}")
    private EstadoProducto estado;
}
