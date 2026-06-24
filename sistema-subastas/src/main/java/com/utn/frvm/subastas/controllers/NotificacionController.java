package com.utn.frvm.subastas.controllers;

import com.utn.frvm.subastas.dtos.NotificacionResponseDTO;
import com.utn.frvm.subastas.services.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Endpoints para la gestión de notificaciones de usuario")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener notificaciones no leídas por usuario ID", description = "Devuelve la lista de notificaciones pendientes para un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<NotificacionResponseDTO>> getUnreadNotifications(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.getUnreadNotifications(usuarioId));
    }

    @PutMapping("/{id}/leer")
    @Operation(summary = "Marcar notificación como leída", description = "Permite marcar una notificación específica como leída")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída exitosamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificacionService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
