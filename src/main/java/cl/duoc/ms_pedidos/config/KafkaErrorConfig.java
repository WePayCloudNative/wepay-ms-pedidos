package cl.duoc.ms_pedidos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Errores del consumidor de audit.timeline (por ejemplo, Oracle no responde):
 * se reintenta 2 veces con 1 segundo de espera y luego se registra y se continúa.
 * No necesita DLT: la confirmación es informativa y la próxima del mismo pedido la reemplaza.
 */
@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (registro, error) -> log.error("No se pudo procesar la confirmación {} de {} (offset {}): {}",
                        registro.key(), registro.topic(), registro.offset(), error.getMessage()),
                new FixedBackOff(1000L, 2L));
    }
}
