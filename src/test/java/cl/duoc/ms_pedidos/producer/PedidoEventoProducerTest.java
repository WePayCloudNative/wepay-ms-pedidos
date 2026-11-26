package cl.duoc.ms_pedidos.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cl.duoc.ms_pedidos.entity.EstadoPedido;
import cl.duoc.ms_pedidos.entity.Pedido;
import cl.duoc.ms_pedidos.event.Actor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Revisa el JSON que viaja por Kafka, sin un Kafka real (KafkaTemplate es un mock). */
class PedidoEventoProducerTest {

    private final ObjectMapper json = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(15L);
        p.setUsuarioOid("oid-cliente");
        p.setTotal(89990.0);
        p.setEstado(EstadoPedido.DESPACHADO);
        p.setFechaRegistro(LocalDateTime.parse("2026-11-24T12:00:00"));
        p.setDetalles(new ArrayList<>());
        return p;
    }

    private JsonNode enviado() {
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("orders.events"), eq("15"), mensaje.capture());
        return json.readTree(mensaje.getValue());
    }

    @Test
    void elEventoDiceQuienHizoElCambioYDesdeQueEstado() {
        new PedidoEventoProducer(kafka, json).publicarEvento(
                pedido(), "OrderDispatched", Actor.de("oid-admin", "ADMIN"), EstadoPedido.EN_PREPARACION);

        JsonNode evento = enviado();
        assertEquals("OrderDispatched", evento.path("tipoEvento").asText());
        assertEquals("oid-cliente", evento.path("usuarioOid").asText()); // dueño del pedido
        assertEquals("oid-admin", evento.path("actorOid").asText());     // quien lo despachó
        assertEquals("ADMIN", evento.path("actorRol").asText());
        assertEquals("EN_PREPARACION", evento.path("estadoAnterior").asText());
        assertEquals("DESPACHADO", evento.path("estado").asText());
    }

    @Test
    void cadaEventoTieneUnIdDistinto() {
        PedidoEventoProducer productor = new PedidoEventoProducer(kafka, json);
        productor.publicarEvento(pedido(), "OrderCreated", Actor.cliente("oid-cliente"), null);

        JsonNode evento = enviado();
        assertTrue(evento.path("eventId").asText().length() >= 32);
        assertTrue(evento.path("estadoAnterior").isNull());
    }
}
