package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.PujaRequestDTO;
import com.utn.frvm.subastas.dtos.PujaResponseDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import com.utn.frvm.subastas.services.PujaService;
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
@RequestMapping("/api/pujas")
@Tag(name = "Pujas", description = "Endpoints para la gestión de ofertas (pujas)")
public class PujaController {

    private final UsuarioRepository usuarioRepository;
    private final PujaService pujaService;

    public PujaController(PujaService pujaService, UsuarioRepository usuarioRepository) {
        this.pujaService = pujaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @Operation(summary = "Realizar una puja", description = "Permite registrar una nueva oferta para una subasta activa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Puja registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio violadas"),
            @ApiResponse(responseCode = "404", description = "Subasta o Comprador no encontrado")
    })

    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PujaResponseDTO> placeBid(@Valid @RequestBody PujaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pujaService.placeBid(request));
    }

    @GetMapping("/subasta/{subastaId}")
    @Operation(summary = "Obtener historial de pujas por subasta ID", description = "Devuelve todas las pujas realizadas en una subasta, ordenadas de mayor a menor monto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial de pujas obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Subasta no encontrada")
    })
    public ResponseEntity<List<PujaResponseDTO>> getBidsBySubastaId( @PathVariable Long subastaId, Authentication authentication) {
        Long usuarioId = extractUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(pujaService.getBidsWithPrivacy(subastaId, usuarioId));
    }

     private Long extractUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return usuario.getId();
    }
}
