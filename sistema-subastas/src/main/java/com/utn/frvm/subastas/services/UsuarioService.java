package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.UsuarioResponseDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO getById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO getByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));
        return mapToResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> getAll() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void blockUser(Long id, String motivo, String adminUsername) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        Usuario admin = usuarioRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Administrador no encontrado con username: " + adminUsername));

        if (admin.getRol() != RolUsuario.ROLE_ADMIN) {
            throw new BusinessRuleException(
                    "Solo un administrador autorizado puede bloquear usuarios.",
                    HttpStatus.FORBIDDEN);
        }

        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuario.setMotivoBloqueo(motivo);
        usuario.setBloqueadoPor(admin);
        usuario.setFechaBloqueo(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void unblockUser(Long id, boolean reducirIncidencias, String adminUsername) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        if (usuario.getEstado() != EstadoUsuario.BLOQUEADO) {
            throw new BusinessRuleException("El usuario no está bloqueado.");
        }
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setBloqueadoPor(null);
        usuario.setFechaBloqueo(null);
        usuario.setMotivoBloqueo(null);
        if (reducirIncidencias && usuario.getIncidenciasAcumuladas() > 0) {
            usuario.setIncidenciasAcumuladas(usuario.getIncidenciasAcumuladas() - 1);
        }
        usuarioRepository.save(usuario);
    }

    boolean isAdmin(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        return usuario != null && usuario.getRol() == RolUsuario.ROLE_ADMIN;
    }

    private UsuarioResponseDTO mapToResponse(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .estado(usuario.getEstado())
                .verificationToken(usuario.getVerificationToken())
                .incidenciasAcumuladas(usuario.getIncidenciasAcumuladas())
                .fechaRegistro(usuario.getFechaRegistro())
                .bloqueadoPorId(usuario.getBloqueadoPor() != null ? usuario.getBloqueadoPor().getId() : null)
                .motivoBloqueo(usuario.getMotivoBloqueo())
                .fechaBloqueo(usuario.getFechaBloqueo())
                .build();
    }
}
