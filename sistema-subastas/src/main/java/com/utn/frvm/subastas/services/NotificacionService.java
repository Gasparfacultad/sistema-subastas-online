package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.NotificacionResponseDTO;
import com.utn.frvm.subastas.entities.Notificacion;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.TipoNotificacion;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.NotificacionRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SubastaRepository subastaRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository, SubastaRepository subastaRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.subastaRepository = subastaRepository;
    }

    @Transactional
    public void sendNotification(Long usuarioId, Long subastaId, TipoNotificacion tipo, String mensaje) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Subasta subasta = null;
        if (subastaId != null) {
            subasta = subastaRepository.findById(subastaId).orElse(null);
        }

        Notificacion notificacion = Notificacion.builder()
                .usuario(usuario)
                .subasta(subasta)
                .tipo(tipo)
                .mensaje(mensaje)
                .leida(false)
                .build();

        notificacionRepository.save(notificacion);
    }

    public List<NotificacionResponseDTO> getUnreadNotifications(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        return notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada con ID: " + id));
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    private NotificacionResponseDTO mapToResponse(Notificacion notificacion) {
        return NotificacionResponseDTO.builder()
                .id(notificacion.getId())
                .usuarioId(notificacion.getUsuario().getId())
                .usuarioUsername(notificacion.getUsuario().getUsername())
                .subastaId(notificacion.getSubasta() != null ? notificacion.getSubasta().getId() : null)
                .subastaTitulo(notificacion.getSubasta() != null ? notificacion.getSubasta().getTitulo() : null)
                .tipo(notificacion.getTipo())
                .mensaje(notificacion.getMensaje())
                .leida(notificacion.getLeida())
                .fechaCreacion(notificacion.getFechaCreacion())
                .build();
    }
}
