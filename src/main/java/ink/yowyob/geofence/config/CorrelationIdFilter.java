package ink.yowyob.geofence.config;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtre WebFlux pour ajouter un Correlation ID à chaque requête
 * Permet de tracer les requêtes à travers les logs
 */
@Component
@Order(-100)
public class CorrelationIdFilter implements WebFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Récupérer le correlation ID du header ou en générer un nouveau
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Ajouter au header de réponse
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        // Stocker dans le context Reactor pour le MDC
        final String finalCorrelationId = correlationId;

        return chain.filter(exchange)
                .contextWrite(context -> {
                    MDC.put(CORRELATION_ID_MDC_KEY, finalCorrelationId);
                    return context.put(CORRELATION_ID_MDC_KEY, finalCorrelationId);
                })
                .doFinally(signalType -> MDC.clear());
    }
}
