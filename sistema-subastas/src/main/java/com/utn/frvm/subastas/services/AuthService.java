package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.LoginRequestDTO;
import com.utn.frvm.subastas.dtos.LoginResponseDTO;
import com.utn.frvm.subastas.dtos.RegisterRequestDTO;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import com.utn.frvm.subastas.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        String rolSolicitado = request.getRolSolicitado();
        RolUsuario rol;
        if ("COMPRADOR".equalsIgnoreCase(rolSolicitado)) {
            rol = RolUsuario.ROLE_USER;
        } else if ("VENDEDOR".equalsIgnoreCase(rolSolicitado)) {
            rol = RolUsuario.ROLE_SELLER;
        } else {
            throw new BusinessRuleException("El rol solicitado no es válido. Solo se permite COMPRADOR o VENDEDOR.");
        }

        if (usuarioRepository.existsByUsername(request.getUsername()) ||
            usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Credenciales no disponibles");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .incidenciasAcumuladas(0)
                .build();

        usuarioRepository.save(usuario);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtUtils.generateToken(authentication);
        return new LoginResponseDTO(token);
    }
}
