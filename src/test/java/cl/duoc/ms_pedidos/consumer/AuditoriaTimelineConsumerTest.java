package cl.duoc.ms_pedidos.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cl.duoc.ms_pedidos.event.EventoAuditadoDTO;
import cl.duoc.ms_pedidos.service.ConfirmacionAuditoriaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/** Mensajes de audit.timeline pasados a mano, sin un Kafka real. */
class AuditoriaTimelineConsumerTest {

    private final ConfirmacionAuditoriaService servicio = mock(ConfirmacionAuditoriaService.class);
    private final AuditoriaTimelineConsumer consumidor =
            new AuditoriaTimelineConsumer(servicio, JsonMapper.builder().build());

    @Test
    void convierteElMensajeEnDtoYLoRegistra() {
        consumidor.recibir("""
                {"eventId":"e-1","tipoEvento":"OrderAccepted","pedidoId":15,"usuarioOid":"oid-cliente",
                 "actorOid":"oid-admin","actorRol":"ADMIN","estadoAnterior":"CREADO","estadoNuevo":"ACEPTADO",
                 "fechaEvento":"2026-11-26T03:00:00","fechaAuditoria":"2026-11-26T03:00:01",
                 "origen":"wepay-ms-auditoria"}
                """);

        ArgumentCaptor<EventoAuditadoDTO> dto = ArgumentCaptor.forClass(EventoAuditadoDTO.class);
        verify(servicio).registrar(dto.capture());
        assertEquals(15L, dto.getValue().pedidoId());
        assertEquals("ACEPTADO", dto.getValue().estadoNuevo());
    }

    @Test
    void unMensajeIlegibleSeDescartaSinDetenerElConsumidor() {
        consumidor.recibir("esto no es json");

        verify(servicio, never()).registrar(any());
    }

    @Test
    void unMensajeSinPedidoSeDescarta() {
        consumidor.recibir("{\"eventId\":\"e-1\",\"fechaAuditoria\":\"2026-11-26T03:00:01\"}");

        verify(servicio, never()).registrar(any());
    }
}
