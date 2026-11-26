package cl.duoc.ms_pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.ms_pedidos.entity.EstadoPedido;
import cl.duoc.ms_pedidos.entity.Pedido;
import cl.duoc.ms_pedidos.event.EventoAuditadoDTO;
import cl.duoc.ms_pedidos.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfirmacionAuditoriaServiceTest {

    private final PedidoRepository repositorio = mock(PedidoRepository.class);
    private final ConfirmacionAuditoriaService servicio = new ConfirmacionAuditoriaService(repositorio);

    private static EventoAuditadoDTO confirmacion(String estado, String fechaAuditoria) {
        return new EventoAuditadoDTO("e-1", "OrderAccepted", 15L, estado,
                LocalDateTime.parse("2026-11-26T03:00:00"), LocalDateTime.parse(fechaAuditoria));
    }

    private static Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(15L);
        p.setEstado(EstadoPedido.ACEPTADO);
        return p;
    }

    @Test
    void marcaElPedidoConLaFechaYElEstadoAuditado() {
        Pedido p = pedido();
        when(repositorio.findById(15L)).thenReturn(Optional.of(p));

        assertTrue(servicio.registrar(confirmacion("ACEPTADO", "2026-11-26T03:00:01")));

        assertEquals(LocalDateTime.parse("2026-11-26T03:00:01"), p.getAuditadoEn());
        assertEquals("ACEPTADO", p.getEstadoAuditado());
        verify(repositorio).save(p);
    }

    @Test
    void noCambiaElEstadoDelPedido() {
        Pedido p = pedido();
        when(repositorio.findById(15L)).thenReturn(Optional.of(p));

        servicio.registrar(confirmacion("CREADO", "2026-11-26T03:00:01"));

        assertEquals(EstadoPedido.ACEPTADO, p.getEstado()); // solo se guarda la confirmación
    }

    @Test
    void unaConfirmacionMasAntiguaNoPisaLaMasReciente() {
        Pedido p = pedido();
        p.setAuditadoEn(LocalDateTime.parse("2026-11-26T03:10:00"));
        p.setEstadoAuditado("ACEPTADO");
        when(repositorio.findById(15L)).thenReturn(Optional.of(p));

        assertFalse(servicio.registrar(confirmacion("CREADO", "2026-11-26T03:00:00")));

        assertEquals("ACEPTADO", p.getEstadoAuditado());
        verify(repositorio, never()).save(any());
    }

    @Test
    void unPedidoInexistenteSeIgnora() {
        when(repositorio.findById(15L)).thenReturn(Optional.empty());

        assertFalse(servicio.registrar(confirmacion("ACEPTADO", "2026-11-26T03:00:01")));
        verify(repositorio, never()).save(any());
    }
}
