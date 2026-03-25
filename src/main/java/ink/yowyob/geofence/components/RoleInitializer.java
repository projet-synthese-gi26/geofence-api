package ink.yowyob.geofence.components;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.model.Role;
import ink.yowyob.geofence.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Vérifier si le rôle UTILISATEUR existe déjà
        if (roleRepository.findByName(UserRole.USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(UserRole.USER);
            roleRepository.save(userRole);
        }

        if (roleRepository.findByName(UserRole.ADMIN).isEmpty()) {
            Role userRole1 = new Role();
            userRole1.setName(UserRole.ADMIN);
            roleRepository.save(userRole1);
        }

        if (roleRepository.findByName(UserRole.MANAGER).isEmpty()) {
            Role userRole1 = new Role();
            userRole1.setName(UserRole.MANAGER);
            roleRepository.save(userRole1);
        }
    }
}
