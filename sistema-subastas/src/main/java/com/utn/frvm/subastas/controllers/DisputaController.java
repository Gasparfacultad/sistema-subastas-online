package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.DisputaRequestDTO;
import com.utn.frvm.subastas.dtos.DisputaResponseDTO;
import com.utn.frvm.subastas.enums.EstadoDisputa;
import com.utn.frvm.subastas.services.DisputaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputas")
@Tag(name = "Disputas", description = "Endpoints para la apertura y resolución de disputas")
public class DisputaController {

    private final DisputaService disputaService;

    public DisputaController(DisputaService disputaService) {
        this.disputaService = disputaService;
    }

    @PostMapping
    @Operation(summary = "Abrir una disputa", description = "Permite a un comprador ganador o vendedor iniciar una disputa sobre una subasta finalizada o adjudicada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Disputa abierta exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio violadas"),
            @ApiResponse(responseCode = "404", description = "Subasta o Usuario no encontrado")
    })
    public ResponseEntity<DisputaResponseDTO> openDispute(@Valid @RequestBody DisputaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disputaService.openDispute(request));
    }

    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver una disputa", description = "Permite a un administrador resolver una disputa a favor del usuario o vendedor (Solo Administradores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disputa resuelta exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio violadas"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Disputa o Administrador no encontrado")
    })
    public ResponseEntity<DisputaResponseDTO> resolveDispute(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam String justificacion,
            @RequestParam EstadoDisputa nuevoEstado) {
        return ResponseEntity.ok(disputaService.resolveDispute(id, adminId, justificacion, nuevoEstado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener disputa por ID", description = "Devuelve los detalles de una disputa dado su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disputa obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Disputa no encontrada")
    })
    public ResponseEntity<DisputaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(disputaService.getById(id));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener disputas por estado", description = "Devuelve la lista de disputas según su estado (Solo Administradores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de disputas obtenida exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<DisputaResponseDTO>> getDisputesByEstado(@PathVariable EstadoDisputa estado) {
        return ResponseEntity.ok(disputaService.getDisputesByEstado(estado));
    }

    @GetMapping("/admin/{adminId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener disputas resueltas por un administrador", description = "Devuelve la lista de disputas resueltas por un administrador específico (Solo Administradores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de disputas obtenida exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<DisputaResponseDTO>> getDisputesByAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(disputaService.getDisputesByAdmin(adminId));
    }
}
