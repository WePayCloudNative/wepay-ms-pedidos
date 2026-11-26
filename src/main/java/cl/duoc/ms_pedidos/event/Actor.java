package cl.duoc.ms_pedidos.event;

import java.util.Set;

/**
 * Quién hizo un cambio en el pedido. Va en cada evento de Kafka para que
 * auditoría pueda responder "¿qué admin despachó este pedido?".
 *
 * rol: ADMIN o CLIENTE. Si no se informa (por ejemplo, una llamada antigua), queda SISTEMA.
 */
public record Actor(String oid, String rol) {

    private static final Set<String> ROLES = Set.of("ADMIN", "CLIENTE");

    public static final Actor SISTEMA = new Actor(null, "SISTEMA");

    public static Actor de(String oid, String rol) {
        if (oid == null || oid.isBlank()) {
            return SISTEMA;
        }
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        return new Actor(oid.trim(), ROLES.contains(normalizado) ? normalizado : "SISTEMA");
    }

    public static Actor cliente(String oid) {
        return de(oid, "CLIENTE");
    }
}
