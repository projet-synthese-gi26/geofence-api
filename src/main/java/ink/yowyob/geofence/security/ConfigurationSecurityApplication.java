package ink.yowyob.geofence.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableJpaAuditing
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ConfigurationSecurityApplication {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            ServerAuthenticationConverter authenticationConverter) {

        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Activer CORS
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // OPTIONS pour toutes les routes (preflight requests)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Authentication endpoints
                        .pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/activation").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Static resources
                        .pathMatchers("/uploads/**").permitAll()

                        // Swagger/OpenAPI documentation
                        .pathMatchers("/v3/api-docs/**").permitAll()
                        .pathMatchers("/swagger-ui.html").permitAll()
                        .pathMatchers("/swagger-ui/**").permitAll()
                        .pathMatchers("/webjars/**").permitAll()

                        // Actuator endpoints (Prometheus monitoring)
                        .pathMatchers("/actuator/**").permitAll()

                        // Redirections
                        .pathMatchers(HttpMethod.GET, "/").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api").permitAll()

                        // Invite links
                        .pathMatchers(HttpMethod.GET, "/api/geofence/invite/*").permitAll()

                        // Location updates from devices
                        .pathMatchers(HttpMethod.POST, "/api/public/location/update").permitAll()

                        // Error handling
                        .pathMatchers("/error").permitAll()

                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}