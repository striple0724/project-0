package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.auth.LoginRequest;
import com.taxworkbench.api.dto.auth.LoginResponse;
import com.taxworkbench.api.dto.user.CreateUserRequest;
import com.taxworkbench.api.dto.user.UpdateUserRequest;
import com.taxworkbench.api.dto.user.UserResponse;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.jpa.entity.UserAccountEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.UserAccountJpaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserAccountService {
    public static final String SESSION_USER_KEY = "LOGIN_USER";

    private final UserAccountJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountJpaRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findBySeq(long seq) {
        UserAccountEntity entity = repository.findById(seq)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + seq));
        return toResponse(entity);
    }

    public UserResponse create(CreateUserRequest request) {
        if (repository.existsById(request.id())) {
            throw new IllegalArgumentException("USER_ID_ALREADY_EXISTS");
        }
        UserAccountEntity entity = new UserAccountEntity();
        entity.setId(request.id());
        entity.setName(request.name());
        entity.setPswd(passwordEncoder.encode(request.password()));
        entity.setEmail(request.email());
        entity.setMobileNo(request.mobileNo());
        entity.setUseYn(normalizeYn(request.useYn()));
        entity.setCrtBy(request.crtBy());
        entity.setCrtIp(request.crtIp());
        entity.setCrtDt(LocalDateTime.now());

        UserAccountEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    public UserResponse update(long seq, UpdateUserRequest request) {
        UserAccountEntity entity = repository.findById(seq)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + seq));

        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setMobileNo(request.mobileNo());
        entity.setUseYn(normalizeYn(request.useYn()));
        entity.setAmnBy(request.amnBy());
        entity.setAmnIp(request.amnIp());
        entity.setAmnDt(LocalDateTime.now());

        if (request.password() != null && !request.password().isBlank()) {
            entity.setPswd(passwordEncoder.encode(request.password()));
        }

        return toResponse(repository.save(entity));
    }

    public void delete(long seq) {
        if (!repository.existsById(seq)) {
            throw new ResourceNotFoundException("User not found: " + seq);
        }
        repository.deleteById(seq);
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        UserAccountEntity entity = repository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!"Y".equalsIgnoreCase(entity.getUseYn())) {
            throw new IllegalArgumentException("ACCOUNT_DISABLED");
        }

        if (!passwordEncoder.matches(request.password(), entity.getPswd())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }

        LoginResponse response = new LoginResponse(entity.getSeq(), entity.getId(), entity.getName(), entity.getUseYn());
        session.setAttribute(SESSION_USER_KEY, response);
        return response;
    }

    @Transactional(readOnly = true)
    public LoginResponse currentUser(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_KEY);
        if (value instanceof LoginResponse loginResponse) {
            return loginResponse;
        }
        throw new IllegalArgumentException("UNAUTHORIZED");
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private String normalizeYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private UserResponse toResponse(UserAccountEntity e) {
        return new UserResponse(
                e.getSeq(),
                e.getId(),
                e.getName(),
                e.getEmail(),
                e.getMobileNo(),
                e.getUseYn(),
                e.getCrtBy(),
                e.getCrtDt(),
                e.getCrtIp(),
                e.getAmnBy(),
                e.getAmnDt(),
                e.getAmnIp()
        );
    }
}
