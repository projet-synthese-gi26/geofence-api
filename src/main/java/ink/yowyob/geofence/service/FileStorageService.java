package ink.yowyob.geofence.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * Méthode pour Spring MVC (MultipartFile)
     */
    public String storeVehicleImage(MultipartFile file, UUID vehicleId) throws IOException {
        // Créer le répertoire de destination s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir + "/vehicles");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Extraire l'extension du fichier original
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Créer le nouveau nom de fichier basé sur l'UUID du véhicule
        String newFilename = vehicleId.toString() + extension;
        Path filePath = uploadPath.resolve(newFilename);

        // Enregistrer le fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Retourner le chemin relatif de l'image
        return "/vehicles/" + newFilename;
    }

    /**
     * Méthode pour WebFlux (FilePart) - version améliorée
     */
    public Mono<String> storeVehicleImageReactive(FilePart filePart, UUID vehicleId) {
        return Mono.fromCallable(() -> {
            try {
                return storeVehicleImageFromFilePart(filePart, vehicleId);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store image", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Méthode pour WebFlux (FilePart)
     */
    public String storeVehicleImageFromFilePart(FilePart filePart, UUID vehicleId) throws IOException {
        // Créer le répertoire de destination s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir + "/vehicles");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Extraire l'extension du fichier original
        String originalFilename = filePart.filename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Créer le nouveau nom de fichier basé sur l'UUID du véhicule
        String newFilename = vehicleId.toString() + extension;
        Path filePath = uploadPath.resolve(newFilename);

        // Enregistrer le fichier de manière synchrone (block nécessaire pour WebFlux)
        filePart.transferTo(filePath).block();

        // Retourner le chemin relatif de l'image
        return "/vehicles/" + newFilename;
    }

    /**
     * Génère l'URL complète de l'image
     * Compatible avec MVC et WebFlux
     */
    public String getCompleteImageUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return null;
        }

        if (!relativeUrl.startsWith("/uploads/")) {
            relativeUrl = "/uploads" + (relativeUrl.startsWith("/") ? "" : "/") + relativeUrl;
        }

        try {
            // Essayer d'obtenir la requête HTTP via ServletRequestAttributes (MVC)
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getRequest();

            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());

            return baseUrl + relativeUrl;
        } catch (Exception e) {
            // Dans un contexte WebFlux, RequestContextHolder pourrait ne pas être disponible
            // Fallback vers une URL par défaut
            return "http://localhost:8080" + relativeUrl;
        }
    }

    /**
     * Supprime un fichier image
     */
    public void deleteVehicleImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                // Nettoyer l'URL pour extraire le chemin relatif
                String relativePath = imageUrl;
                if (imageUrl.startsWith("http")) {
                    // Extraire le chemin après le domaine
                    int uploadsIndex = imageUrl.indexOf("/uploads/");
                    if (uploadsIndex != -1) {
                        relativePath = imageUrl.substring(uploadsIndex);
                    }
                }

                Path imagePath = Paths.get(uploadDir + relativePath);
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                // Ignorer l'erreur si le fichier n'existe pas ou ne peut pas être supprimé
            }
        }
    }
}