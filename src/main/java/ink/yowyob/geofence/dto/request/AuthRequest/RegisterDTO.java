package ink.yowyob.geofence.dto.request.AuthRequest;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record RegisterDTO(
        String firstname,
        String lastname,
        String username,
        String phoneNumber,
        @Pattern(regexp = "^[^@]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Votre email n'est pas correct")
        String email,
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial")
        String password,
        String password_confirmation,
        LocalDate DOB
) {
}
