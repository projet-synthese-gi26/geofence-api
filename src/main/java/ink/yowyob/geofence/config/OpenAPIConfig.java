package ink.yowyob.geofence.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI geofenceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Geofence - Gestion de Zones Géographiques")
                        .description("""
                                ## API REST pour la gestion de geofencing multi-tenant

                                Cette API permet de :
                                - **Gérer des véhicules** et leur suivi GPS en temps réel
                                - **Créer des zones géographiques** (cercles et polygones)
                                - **Générer des alertes automatiques** lors d'entrées/sorties de zones
                                - **Définir des routes autorisées** et détecter les déviations
                                - **Isoler les données** par tenant (multi-tenant)

                                ### Authentification
                                L'API utilise JWT (JSON Web Tokens) pour l'authentification. Incluez le token dans l'en-tête Authorization :
                                ```
                                Authorization: Bearer <votre-token-jwt>
                                ```

                                ### Multi-tenant
                                Chaque requête doit inclure l'en-tête `X-Tenant-ID` pour isoler les données :
                                ```
                                X-Tenant-ID: <votre-tenant-id>
                                ```

                                ### Formats de coordonnées
                                Les coordonnées géographiques utilisent le format **GeoJSON** :
                                - Longitude en premier, latitude en second : `[longitude, latitude]`
                                - Exemple : `[2.3522, 48.8566]` pour Paris
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Équipe Geofence")
                                .email("support@geofence.com")
                                .url("https://geofence.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Serveur de développement local"),
                        new Server()
                                .url("https://api.geofence.com")
                                .description("Serveur de production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .name("bearer-jwt")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Authentification JWT. Obtenez votre token via `/api/auth/login`"))
                        .addSecuritySchemes("tenant-id", new SecurityScheme()
                                .name("X-Tenant-ID")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Identifiant du tenant pour l'isolation multi-tenant")));
    }
}
