package com.sigi.integration.config;

import com.sigi.persistence.entity.Role;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.RoleRepository;
import com.sigi.persistence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestDataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            Role adminRole = roleRepository.findByName(RoleList.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role r = Role.builder().name(RoleList.ROLE_ADMIN).build();
                        return roleRepository.save(r);
                    });

            if (userRepository.findByEmail("admin@sigi.com").isEmpty()) {
                User admin = User.builder()
                        .name("Admin")
                        .surname("Admin")
                        .email("admin@sigi.com")
                        .phoneNumber("3000000000")
                        .password(passwordEncoder.encode("123456789"))
                        .role(adminRole) // role ya persistido o recuperado
                        .active(true)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}