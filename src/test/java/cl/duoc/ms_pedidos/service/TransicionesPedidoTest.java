package cl.duoc.ms_pedidos.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.ms_pedidos.entity.EstadoPedido;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El flujo del pedido: qué cambios de estado se permiten y cuáles no. */
class TransicionesPedidoTest {

    @ParameterizedTest(name = "{0} -> {1} está permitido")
    @CsvSource({
            "CREADO, ACEPTADO",
            "CREADO, CANCELADO",
            "ACEPTADO, EN_PREPARACION",
            "ACEPTADO, CANCELADO",
            "EN_PREPARACION, DESPACHADO",
            "EN_PREPARACION, CANCELADO",
            "DESPACHADO, ENTREGADO"
    })
    void transicionesPermitidas(EstadoPedido desde, EstadoPedido hacia) {
        assertDoesNotThrow(() -> PedidoService.validarTransicion(desde, hacia));
    }

    @ParameterizedTest(name = "{0} -> {1} está prohibido")
    @CsvSource({
            "CREADO, DESPACHADO",         // no se puede saltar el pago
            "ACEPTADO, ENTREGADO",
            "DESPACHADO, CANCELADO",      // ya salió: no se cancela
            "ENTREGADO, CANCELADO",
            "CANCELADO, ACEPTADO",        // un cancelado no revive
            "ENTREGADO, CREADO",
            "ACEPTADO, ACEPTADO"          // mismo estado
    })
    void transicionesProhibidas(EstadoPedido desde, EstadoPedido hacia) {
        assertThrows(IllegalStateException.class, () -> PedidoService.validarTransicion(desde, hacia));
    }
}
