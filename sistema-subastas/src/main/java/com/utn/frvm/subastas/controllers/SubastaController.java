package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.SubastaRequestDTO;
import com.utn.frvm.subastas.dtos.SubastaResponseDTO;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.services.SubastaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subastas")
@Tag(name = "Subastas", description = "Endpoints para la gestión de subastas")
public class SubastaController {

    private final SubastaService subastaService;

    public SubastaController(SubastaService subastaService) {
        this.subastaService = subastaService;
    }

    @PostMapping
    @Operation(summary = "Crear una subasta", description = "Permite registrar una nueva subasta en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subasta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Vendedor o Producto no encontrado")
    })
    public ResponseEntity<SubastaResponseDTO> create(@Valid @RequestBody SubastaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subastaService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener subasta por ID", description = "Devuelve los detalles de una subasta dado su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subasta obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Subasta no encontrada")
    })
    public ResponseEntity<SubastaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subastaService.getById(id));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @Operation(summary = "Obtener subastas por vendedor ID", description = "Devuelve la lista de subastas de un vendedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de subastas obtenida exitosamente")
    })
    public ResponseEntity<List<SubastaResponseDTO>> getByVendedorId(@PathVariable Long vendedorId) {
        return ResponseEntity.ok(subastaService.getByVendedorId(vendedorId));
    }

    @GetMapping("/estado/{estado}")
    @Operation(summary = "Obtener subastas por estado", description = "Devuelve la lista de subastas según su estado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de subastas obtenida exitosamente")
    })
    public ResponseEntity<List<SubastaResponseDTO>> getByEstado(@PathVariable EstadoSubasta estado) {
        return ResponseEntity.ok(subastaService.getByEstado(estado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar subasta", description = "Permite modificar los detalles de una subasta existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subasta actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Subasta no encontrada")
    })
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody SubastaRequestDTO request) {
        subastaService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar subasta", description = "Cierra la subasta adjudicando al ganador actual si existe o finalizándola")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subasta cerrada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Subasta no encontrada")
    })
    public ResponseEntity<Void> closeAuction(@PathVariable Long id) {
        subastaService.closeAuction(id);
        return ResponseEntity.ok().build();
    }
}
