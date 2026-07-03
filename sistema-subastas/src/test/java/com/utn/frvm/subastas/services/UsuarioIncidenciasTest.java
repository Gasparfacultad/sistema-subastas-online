package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UsuarioIncidenciasTest {

    @Test
    void shouldBlockUserOnThirdIncident() {
        Usuario admin = Usuario.builder().id(1L).username("admin").build();
        Usuario usuario = Usuario.builder()
                .id(2L)
                .username("usuario")
                .estado(EstadoUsuario.ACTIVO)
                .incidenciasAcumuladas(0)
                .build();

        // Primera incidencia
        usuario.registrarIncidencia(admin, "Primera infraccion");
        assertEquals(1, usuario.getIncidenciasAcumuladas());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertNull(usuario.getBloqueadoPor());

        // Segunda incidencia
        usuario.registrarIncidencia(admin, "Segunda infraccion");
        assertEquals(2, usuario.getIncidenciasAcumuladas());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertNull(usuario.getBloqueadoPor());

        // Tercera incidencia -> Se debe bloquear automaticamente
        usuario.registrarIncidencia(admin, "Tercera infraccion");
        assertEquals(3, usuario.getIncidenciasAcumuladas());
        assertEquals(EstadoUsuario.BLOQUEADO, usuario.getEstado());
        assertEquals(admin, usuario.getBloqueadoPor());
        assertNotNull(usuario.getFechaBloqueo());
        assertEquals("Acumulación de 3 incidencias: Tercera infraccion", usuario.getMotivoBloqueo());
    }
}
