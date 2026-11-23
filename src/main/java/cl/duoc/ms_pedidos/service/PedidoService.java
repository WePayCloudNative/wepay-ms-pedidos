package cl.duoc.ms_pedidos.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.duoc.ms_pedidos.config.RabbitMQConfig;
import cl.duoc.ms_pedidos.dto.DetalleResponse;
import cl.duoc.ms_pedidos.dto.ItemCarritoRequest;
import cl.duoc.ms_pedidos.dto.PedidoRequest;
import cl.duoc.ms_pedidos.dto.PedidoResponse;
import cl.duoc.ms_pedidos.entity.DetallePedido;
import cl.duoc.ms_pedidos.entity.EstadoPedido;
import cl.duoc.ms_pedidos.entity.Pedido;
import cl.duoc.ms_pedidos.producer.PedidoEventoProducer;
import cl.duoc.ms_pedidos.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PedidoEventoProducer pedidoEventoProducer;

    public PedidoService(PedidoRepository pedidoRepository, RabbitTemplate rabbitTemplate,
            PedidoEventoProducer pedidoEventoProducer) {
        this.pedidoRepository = pedidoRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.pedidoEventoProducer = pedidoEventoProducer;
    }

    @Transactional
    public PedidoResponse crearPedido(String oid, PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioOid(oid);
        pedido.setFechaRegistro(LocalDateTime.now());
        // Regla de negocio: Todo pedido inicia en estado CREADO
        pedido.setEstado(EstadoPedido.CREADO);

        double totalCalculado = 0.0;

        for (ItemCarritoRequest item : request.items()) {
            DetallePedido detalle = new DetallePedido(
                    item.productoId(),
                    item.nombre(),
                    item.precio(),
                    item.cantidad());
            pedido.getDetalles().add(detalle);

            totalCalculado += item.precio() * item.cantidad();
        }

        pedido.setTotal(totalCalculado);

        Pedido guardado = pedidoRepository.save(pedido);

        publicarDespuesDeGuardar(
                guardado,
                "OrderCreated",
                null);

        return convertirADto(guardado);

    }

    @Transactional
    public PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // Regla de negocio: solo se permiten las transiciones de validarTransicion()
        validarTransicion(
                pedido.getEstado(),
                nuevoEstado);

        // Se guarda el estado anterior: facturación lo necesita para saber si un
        // pedido cancelado ya tenía boleta (y entonces corresponde nota de crédito).
        EstadoPedido estadoAnterior = pedido.getEstado();

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);

        // TODO: (Próximamente) Si el estado es ACEPTADO, llamar a ms-catalogo para
        // descontar stock

        String tipoEvento = switch (nuevoEstado) {
            case CREADO -> "OrderCreated";
            case ACEPTADO -> "OrderAccepted";
            case EN_PREPARACION -> "OrderPreparing";
            case DESPACHADO -> "OrderDispatched";
            case ENTREGADO -> "OrderDelivered";
            case CANCELADO -> "OrderCancelled";
        };

        publicarDespuesDeGuardar(
                actualizado,
                tipoEvento,
                estadoAnterior);

        return convertirADto(actualizado);

    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> obtenerTodos() {
        return pedidoRepository.findAll().stream()
                .map(this::convertirADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> obtenerPorUsuario(String oid) {
        return pedidoRepository.findByUsuarioOid(oid).stream()
                .map(this::convertirADto)
                .toList();
    }

    private PedidoResponse convertirADto(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getUsuarioOid(),
                p.getTotal(),
                p.getFechaRegistro(),
                p.getEstado().name(),
                p.getDetalles().stream()
                        .map(d -> new DetalleResponse(
                                d.getProductoId(),
                                d.getNombreProducto(),
                                d.getPrecioUnitario(),
                                d.getCantidad()))
                        .toList());
    }

    private void publicarDespuesDeGuardar(
            Pedido pedido,
            String tipoEvento,
            EstadoPedido estadoAnterior) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        // El pedido ya está guardado: una falla al publicar se registra,
                        // pero no se propaga al cliente ni impide la otra publicación.

                        // Kafka: evento de dominio completo (auditoría, reportes)
                        try {
                            pedidoEventoProducer.publicarEvento(pedido, tipoEvento);
                        } catch (Exception e) {
                            System.err.println("[ERROR] No se pudo publicar " + tipoEvento
                                    + " en Kafka para el pedido #" + pedido.getId() + ": " + e.getMessage());
                        }

                        // RabbitMQ: comando de notificación (email, sms, facturación)
                        try {
                            publicarNotificacion(pedido, estadoAnterior);
                        } catch (Exception e) {
                            System.err.println("[ERROR] No se pudo publicar en RabbitMQ el pedido #"
                                    + pedido.getId() + ": " + e.getMessage());
                        }
                    }
                });
    }

    /**
     * Publica el cambio de estado en el exchange ex.pedidos con routing key
     * "pedido.estado.<estado>". El productor no conoce las colas: los bindings
     * deciden si el mensaje va a email, sms y/o facturación.
     * El estado CREADO no se publica en RabbitMQ (solo en Kafka).
     */
    private void publicarNotificacion(Pedido pedido, EstadoPedido estadoAnterior) {
        EstadoPedido estado = pedido.getEstado();
        if (estado == EstadoPedido.CREADO) {
            return;
        }

        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("pedidoId", pedido.getId());
        mensaje.put("estado", estado.name());
        if (estadoAnterior != null) {
            mensaje.put("estadoAnterior", estadoAnterior.name());
        }
        mensaje.put("total", pedido.getTotal());
        // Simulamos el correo usando el OID del usuario
        mensaje.put("to", "cliente_" + pedido.getUsuarioOid() + "@wepay.cl");
        mensaje.put("subject", "Actualización de Pedido #" + pedido.getId());
        mensaje.put("body", "Tu pedido ha cambiado al estado: " + estado.name());

        String routingKey = RabbitMQConfig.RK_ESTADO + estado.name().toLowerCase();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EX_PEDIDOS, routingKey, mensaje);
        System.out.println(" [OK] Evento publicado en " + RabbitMQConfig.EX_PEDIDOS
                + " con routing key " + routingKey);
    }

    private void validarTransicion(
            EstadoPedido estadoActual,
            EstadoPedido nuevoEstado) {

        if (estadoActual == nuevoEstado) {
            throw new IllegalStateException(
                    "El pedido ya se encuentra en estado: "
                            + estadoActual);
        }

        boolean permitido = switch (estadoActual) {

            case CREADO ->
                nuevoEstado == EstadoPedido.ACEPTADO
                        || nuevoEstado == EstadoPedido.CANCELADO;

            case ACEPTADO ->
                nuevoEstado == EstadoPedido.EN_PREPARACION
                        || nuevoEstado == EstadoPedido.CANCELADO;

            case EN_PREPARACION ->
                nuevoEstado == EstadoPedido.DESPACHADO
                        || nuevoEstado == EstadoPedido.CANCELADO;

            case DESPACHADO ->
                nuevoEstado == EstadoPedido.ENTREGADO;

            case ENTREGADO, CANCELADO -> false;
        };

        if (!permitido) {
            throw new IllegalStateException(
                    "Transición no permitida: "
                            + estadoActual + " -> " + nuevoEstado);
        }
    }

}