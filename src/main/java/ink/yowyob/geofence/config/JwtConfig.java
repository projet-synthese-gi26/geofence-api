package ink.yowyob.geofence.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class JwtConfig {

    @Value("${jwt.encryption.key}")
    private String encryptionKey;

}

