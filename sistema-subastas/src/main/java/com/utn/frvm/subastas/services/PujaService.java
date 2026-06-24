package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.PujaRequestDTO;
import com.utn.frvm.subastas.dtos.PujaResponseDTO;
import com.utn.frvm.subastas.entities.Puja;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.PujaRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PujaService {

    private final PujaRepository pujaRepository;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;

    public PujaService(PujaRepository pujaRepository, SubastaRepository subastaRepository,
            UsuarioRepository usuarioRepository) {
        this.pujaRepository = pujaRepository;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PujaResponseDTO realizarPuja(PujaRequestDTO dto, Long compradorId) {
        Subasta subasta = subastaRepository.findByIdForUpdate(dto.getSubastaId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Subasta no encontrada con ID: " + dto.getSubastaId()));

        Usuario comprador = usuarioRepository.findById(compradorId)
                .orElseThrow(() -> new ResourceNotFoundException("Comprador no encontrado con ID: " + compradorId));
        
        if (comprador.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException(
            "El usuario se encuentra bloqueado o inactivo.");
        }
        
        if (subasta.getFechaCierre().isBefore(LocalDateTime.now())) {
        throw new BusinessRuleException(
            "La subasta ya se encuentra cerrada.");
        }

        if (subasta.getVendedor().getId().equals(comprador.getId())) {
            throw new BusinessRuleException("El vendedor no puede ofertar en su propia subasta.");
        }

        if (subasta.getEstado() != EstadoSubasta.ACTIVA) {
            throw new BusinessRuleException("La subasta no está activa.");
        }

        BigDecimal threshold = subasta.getMontoActual().add(subasta.getIncrementoMinimoPuja());
        if (dto.getMonto().compareTo(threshold) <= 0) {
            throw new BusinessRuleException("El monto de la puja debe ser estrictamente mayor a " + threshold);
        }

        Puja nuevaPuja = Puja.builder()
                .subasta(subasta)
                .comprador(comprador)
                .monto(dto.getMonto())
                .esGanadora(true)
                .build();

        Puja savedPuja = pujaRepository.save(nuevaPuja);
        pujaRepository.updatePreviousWinningBidsToFalse(subasta.getId(), savedPuja.getId());

        subasta.setMontoActual(dto.getMonto());
        subasta.setGanadorActual(comprador);
        subastaRepository.save(subasta);

        return mapToResponse(savedPuja);
    }

    @Transactional
    public PujaResponseDTO placeBid(PujaRequestDTO request) {
        return realizarPuja(request, request.getCompradorId());
    }

    public List<PujaResponseDTO> getBidsBySubastaId(Long subastaId) {
        if (!subastaRepository.existsById(subastaId)) {
            throw new ResourceNotFoundException("Subasta no encontrada con ID: " + subastaId);
        }
        return pujaRepository.findBySubastaIdOrderByMontoDesc(subastaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PujaResponseDTO mapToResponse(Puja puja) {
        return PujaResponseDTO.builder()
                .id(puja.getId())
                .subastaId(puja.getSubasta().getId())
                .compradorId(puja.getComprador().getId())
                .compradorUsername(puja.getComprador().getUsername())
                .monto(puja.getMonto())
                .fechaHora(puja.getFechaHora())
                .esGanadora(puja.getEsGanadora())
                .build();
    }
}
