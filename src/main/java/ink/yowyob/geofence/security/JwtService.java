package ink.yowyob.geofence.security;

import ink.yowyob.geofence.config.JwtConfig;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.service.Implementation.UserServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@AllArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;
    private UserServiceImpl userService;

    public Map<String, String> generate(String username) {
        User user = this.userService.loadUserByUsername(username);
        return this.generateJwt(user);
    }

    public String extractUsername(String token) {
        return this.getClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        return getClaim(token, claims -> {
            List<?> roles = claims.get("roles", List.class);
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream().map(String::valueOf).toList();
        });
    }

    public String extractTenantId(String token) {
        return getClaim(token, claims -> claims.get("tenant_id", String.class));
    }

    public UUID extractUserId(String token) {
        String userId = getClaim(token, claims -> claims.get("user_id", String.class));
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return UUID.fromString(userId);
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private Date getExpirationDateFromToken(String token) {
        return this.getClaim(token, Claims::getExpiration);
    }

    private <T> T getClaim(String token, Function<Claims, T> function) {
        Claims claims = getAllClaims(token);
        return function.apply(claims);
    }

    private Claims getAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(this.getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new JwtAuthenticationException(e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        return !getAllClaims(token).isEmpty();
    }

    private Map<String, String> generateJwt(User user) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + 30 * 60 * 1000; // 30 minutes

        final Map<String, Object> claims = Map.of(
                "nom", user.getFirstname(),
                "roles", List.of(user.getRole().getName().toString()),
            "tenant_id", user.getOrganization() != null ? user.getOrganization().getApiKey() : "default",
            "user_id", user.getUuid().toString(),
                Claims.EXPIRATION, new Date(expirationTime),
                Claims.SUBJECT, user.getUsername()
        );

        final String bearer = Jwts.builder()
                .setIssuedAt(new Date(currentTime))
                .setExpiration(new Date(expirationTime))
                .setSubject(user.getUsername())
                .setClaims(claims)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        return Map.of("bearer", bearer);
    }

    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(jwtConfig.getEncryptionKey());
        return Keys.hmacShaKeyFor(decoder);
    }
}