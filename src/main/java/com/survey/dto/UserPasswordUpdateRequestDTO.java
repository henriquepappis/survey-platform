package com.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserPasswordUpdateRequestDTO {

    @NotBlank
    @Size(min = 6, max = 255)
    private String newPassword;

    public UserPasswordUpdateRequestDTO() {
    }

    public UserPasswordUpdateRequestDTO(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
