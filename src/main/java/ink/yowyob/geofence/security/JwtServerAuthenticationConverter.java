package ink.yowyob.geofence.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final JwtService jwtService;
    private static final String BEARER = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER))
                .map(header -> header.substring(BEARER.length()))
                .flatMap(token -> validateTenantConsistency(exchange, token).thenReturn(token))
                .map(token -> new JwtToken(token, createUserDetails(token)));
    }

    private Mono<Void> validateTenantConsistency(ServerWebExchange exchange, String token) {
        String requestTenant = exchange.getRequest().getHeaders().getFirst(TenantConstants.TENANT_HEADER);
        if (requestTenant == null || requestTenant.isBlank()) {
            return Mono.empty();
        }

        String tokenTenant = jwtService.extractTenantId(token);
        if (tokenTenant == null || tokenTenant.isBlank()) {
            return Mono.empty();
        }

        if (!requestTenant.equals(tokenTenant)) {
            return Mono.error(new JwtAuthenticationException("Tenant header does not match token tenant."));
        }

        return Mono.empty();
    }

    private UserDetails createUserDetails(String token) {
        String username = jwtService.extractUsername(token);
        return User.builder()
                .username(username)
                .authorities(createAuthorities(token))
                .password("")
                .build();
    }

    private List<SimpleGrantedAuthority> createAuthorities(String token) {
        return jwtService.extractRoles(token).stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}