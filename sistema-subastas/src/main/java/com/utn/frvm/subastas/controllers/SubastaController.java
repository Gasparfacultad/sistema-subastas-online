package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.SubastaRequestDTO;
import com.utn.frvm.subastas.dtos.SubastaResponseDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import com.utn.frvm.subastas.services.SubastaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subastas")
@Tag(name = "Subastas", description = "Endpoints para la gestión de subastas")
public class SubastaController {

    private final SubastaService subastaService;
    private final UsuarioRepository usuarioRepository;

    public SubastaController(SubastaService subastaService, UsuarioRepository usuarioRepository) {
        this.subastaService = subastaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @Operation(summary = "Crear una subasta", description = "Permite registrar una nueva subasta en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subasta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Vendedor o Producto no encontrado")
    })

    @PreAuthorize("hasRole('SELLER')")
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

    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody SubastaRequestDTO request, Authentication authentication) {
        Long usuarioId = extractUserIdFromAuthentication(authentication);
        subastaService.update(id, request, usuarioId);
        return ResponseEntity.ok().build();
    }
    // Método auxiliar para extraer ID del usuario autenticado
    private Long extractUserIdFromAuthentication(Authentication authentication) {
    // Implementación: extraer ID del token JWT o del contexto de seguridad
    // Opción 1: Buscar usuario por username
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return usuario.getId();
    }


    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar subasta", description = "Cierra la subasta adjudicando al ganador actual si existe o finalizándola")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subasta cerrada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Subasta no encontrada")
    })

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> closeAuction(@PathVariable Long id, Authentication authentication) {
        Long adminId = extractUserIdFromAuthentication(authentication);
        subastaService.closeAuction(id, adminId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Void> cancelAuction(
        @PathVariable Long id,
        @RequestParam String motivo,
        Authentication authentication) {
        
        Long usuarioId = extractUserIdFromAuthentication(authentication);
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        
        subastaService.cancelAuction(id, motivo, usuarioId, usuario.getRol());
        return ResponseEntity.ok().build();
    }


}
