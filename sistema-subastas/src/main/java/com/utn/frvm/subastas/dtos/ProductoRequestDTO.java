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

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    @NotNull(message = "El vendedor es obligatorio")
    private Long vendedorId;

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    @Size(min = 3, max = 150, message = "El nombre del producto debe tener entre 3 y 150 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "El estado del producto es obligatorio")
    private EstadoProducto estado;
}
