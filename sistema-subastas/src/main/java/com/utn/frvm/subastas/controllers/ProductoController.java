package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.ProductoRequestDTO;
import com.utn.frvm.subastas.dtos.ProductoResponseDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoProducto;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import com.utn.frvm.subastas.services.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Endpoints para la gestión de productos")
public class ProductoController {

    private final ProductoService productoService;
    private final UsuarioRepository usuarioRepository;

    public ProductoController(ProductoService productoService, UsuarioRepository usuarioRepository) {
        this.productoService = productoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @Operation(summary = "Crear un producto", description = "Permite registrar un nuevo producto en el catálogo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría o Vendedor no encontrado")
    })

    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductoResponseDTO> create(@Valid @RequestBody ProductoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Devuelve los detalles de un producto dado su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductoResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.getById(id));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @Operation(summary = "Obtener productos por vendedor ID", description = "Devuelve la lista de productos de un vendedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    })
    public ResponseEntity<List<ProductoResponseDTO>> getByVendedorId(@PathVariable Long vendedorId) {
        return ResponseEntity.ok(productoService.getByVendedorId(vendedorId));
    }

    @GetMapping("/estado/{estado}")
    @Operation(summary = "Obtener productos por estado", description = "Devuelve la lista de productos según su estado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    })
    public ResponseEntity<List<ProductoResponseDTO>> getByEstado(@PathVariable EstadoProducto estado) {
        return ResponseEntity.ok(productoService.getByEstado(estado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Permite modificar los detalles de un producto existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Producto o Categoría no encontrados")
    })

    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody ProductoRequestDTO request, Authentication authentication) {
        // Extraer el ID del usuario autenticado
        Long usuarioId = extractUserIdFromAuthentication(authentication);
        productoService.update(id, request, usuarioId);
        return ResponseEntity.ok().build();
        }

        // Método auxiliar (puedes ponerlo en un servicio o en el controller)
        private Long extractUserIdFromAuthentication(Authentication authentication) {
            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            return usuario.getId();
        }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> delete( @PathVariable Long id, Authentication authentication) {
        
        Long usuarioId = extractUserIdFromAuthentication(authentication);
        productoService.delete(id, usuarioId);
        return ResponseEntity.noContent().build();
    }    
}
