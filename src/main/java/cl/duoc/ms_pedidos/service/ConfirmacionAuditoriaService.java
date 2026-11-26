package cl.duoc.ms_pedidos.service;

import cl.duoc.ms_pedidos.entity.Pedido;
import cl.duoc.ms_pedidos.event.EventoAuditadoDTO;
import cl.duoc.ms_pedidos.repository.PedidoRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca un pedido como auditado cuando llega la confirmación de ms-auditoria.
 * No cambia el estado del pedido ni publica eventos: solo guarda la confirmación.
 */
@Service
public class ConfirmacionAuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionAuditoriaService.class);

    private final PedidoRepository pedidoRepository;

    public ConfirmacionAuditoriaService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /** @return true si se actualizó el pedido */
    @Transactional
    public boolean registrar(EventoAuditadoDTO confirmacion) {
        Optional<Pedido> encontrado = pedidoRepository.findById(confirmacion.pedidoId());
        if (encontrado.isEmpty()) {
            log.warn("Confirmación de auditoría para un pedido que no existe: #{}", confirmacion.pedidoId());
            return false;
        }

        Pedido pedido = encontrado.get();
        // Si llega una confirmación más antigua que la guardada (por ejemplo, al releer el tópico),
        // no se pisa la más reciente
        if (pedido.getAuditadoEn() != null && confirmacion.fechaAuditoria().isBefore(pedido.getAuditadoEn())) {
            return false;
        }

        pedido.setAuditadoEn(confirmacion.fechaAuditoria());
        pedido.setEstadoAuditado(confirmacion.estadoNuevo());
        pedidoRepository.save(pedido);
        log.info("Pedido #{} auditado en estado {}", pedido.getId(), confirmacion.estadoNuevo());
        return true;
    }
}
