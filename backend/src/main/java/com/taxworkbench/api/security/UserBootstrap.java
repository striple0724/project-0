package com.taxworkbench.api.security;

import com.taxworkbench.api.infrastructure.jpa.entity.UserAccountEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.UserAccountJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserBootstrap implements CommandLineRunner {
    private final UserAccountJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserBootstrap(UserAccountJpaRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        UserAccountEntity admin = new UserAccountEntity();
        admin.setId("admin");
        admin.setName("관리자");
        admin.setPswd(passwordEncoder.encode("admin1234"));
        admin.setEmail("admin");
        admin.setMobileNo("01000000000");
        admin.setUseYn("Y");
        admin.setCrtBy("system");
        admin.setCrtIp("127.0.0.1");
        admin.setCrtDt(LocalDateTime.now());

        repository.save(admin);
    }
}
