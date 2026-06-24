package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.UsuarioResponseDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
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

    public UsuarioResponseDTO getById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToResponse(usuario);
    }

    public UsuarioResponseDTO getByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));
        return mapToResponse(usuario);
    }

    public List<UsuarioResponseDTO> getAll() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void blockUser(Long id, String motivo, Long adminId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con ID: " + adminId));

        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuario.setMotivoBloqueo(motivo);
        usuario.setBloqueadoPor(admin);
        usuario.setFechaBloqueo(LocalDateTime.now());
        usuarioRepository.save(usuario);
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
