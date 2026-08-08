package com.ka.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank @Size(min = 3, max = 20)
    private String username;
    @NotBlank @Size(min = 6, max = 32)
    private String password;
    @Email
    private String email;
}
