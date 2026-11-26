package cl.duoc.ms_pedidos.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void normalizaElRol() {
        assertEquals(new Actor("oid-1", "ADMIN"), Actor.de(" oid-1 ", "admin"));
    }

    @Test
    void unRolDesconocidoQuedaComoSistema() {
        assertEquals("SISTEMA", Actor.de("oid-1", "SUPERUSUARIO").rol());
    }

    @Test
    void sinOidEsElSistema() {
        assertEquals(Actor.SISTEMA, Actor.de(null, "ADMIN"));
        assertEquals(Actor.SISTEMA, Actor.de("  ", "ADMIN"));
    }
}
