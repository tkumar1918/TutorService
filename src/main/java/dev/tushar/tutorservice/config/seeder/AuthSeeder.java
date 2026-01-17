package dev.tushar.tutorservice.config.seeder;

import dev.tushar.tutorservice.entity.Permission;
import dev.tushar.tutorservice.entity.Role;
import dev.tushar.tutorservice.entity.User;
import dev.tushar.tutorservice.model.PermissionKey;
import dev.tushar.tutorservice.repository.PermissionRepository;
import dev.tushar.tutorservice.repository.RoleRepository;
import dev.tushar.tutorservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(1)
@Transactional
@RequiredArgsConstructor
public class AuthSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String @NonNull ... args) {
        // --- Create All Permissions from the Enum ---
        for (PermissionKey p : PermissionKey.values()) {
            createPermissionIfNotFound(p.name());
        }

        // --- Create Role-Permission Mappings ---
        createRoleIfNotFound("USER", Set.of(
                PermissionKey.READ_TUTOR_DATA, PermissionKey.WRITE_TUTOR_DATA));
        createRoleIfNotFound("ADMIN", Set.of(PermissionKey.values()));

        // --- Create Default User Accounts ---
        createAdminUserIfNotFound();
        createUserIfNotFound();
    }

    private void createAdminUserIfNotFound() {
        String adminEmail = "admin@tutor.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(
                    () -> new RuntimeException("Admin role not found"));
            User adminUser = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin/1234"))
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(adminUser);
        }
    }

    private void createUserIfNotFound() {
        String userEmail = "student@tutor.com";
        if (!userRepository.existsByEmail(userEmail)) {
            Role userRole = roleRepository.findByName("USER").orElseThrow(
                    () -> new RuntimeException("User role not found"));
            User regularUser = User.builder()
                    .firstName("Student")
                    .lastName("User")
                    .email(userEmail)
                    .passwordHash(passwordEncoder.encode("Student/1234"))
                    .roles(Set.of(userRole))
                    .build();
            userRepository.save(regularUser);
        }
    }

    private void createPermissionIfNotFound(String name) {
        permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(Permission.builder().name(name).build()));
    }

    private void createRoleIfNotFound(String name, Set<PermissionKey> permissions) {
        roleRepository.findByName(name).orElseGet(() -> {
            Set<Permission> perms = permissions.stream()
                    .map(p -> permissionRepository.findByName(p.name()).orElseThrow(
                            () -> new RuntimeException("Permission not found")))
                    .collect(Collectors.toSet());
            Role role = Role.builder().name(name).permissions(perms).build();
            return roleRepository.save(role);
        });
    }
}
