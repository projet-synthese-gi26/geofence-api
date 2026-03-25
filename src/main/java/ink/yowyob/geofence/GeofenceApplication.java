package ink.yowyob.geofence;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@SpringBootApplication(exclude = {
		SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class,
		WebMvcAutoConfiguration.class,
		DispatcherServletAutoConfiguration.class
})
public class GeofenceApplication {

	public static void main(String[] args) {
		// Charger le .env AVANT de démarrer Spring Boot
		loadDotenv();

		SpringApplication app = new SpringApplication(GeofenceApplication.class);
		app.setWebApplicationType(org.springframework.boot.WebApplicationType.REACTIVE);
		app.run(args);
	}

	private static void loadDotenv() {
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.ignoreIfMalformed()
					.ignoreIfMissing()
					.load();

			// Charger les variables d'environnement
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});

			System.out.println("Variables d'environnement chargées depuis .env");
		} catch (Exception e) {
			System.out.println("Aucun fichier .env trouvé ou erreur de chargement: " + e.getMessage());
		}
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
		corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		corsConfig.setAllowedHeaders(Arrays.asList("*"));
		corsConfig.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
		corsConfig.setAllowCredentials(false);
		corsConfig.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsWebFilter(source);
	}
}