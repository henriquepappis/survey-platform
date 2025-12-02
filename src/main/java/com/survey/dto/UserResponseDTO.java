package com.survey.dto;

import java.time.LocalDateTime;

public class UserResponseDTO {

    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;

    public UserResponseDTO(Long id, String username, String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
