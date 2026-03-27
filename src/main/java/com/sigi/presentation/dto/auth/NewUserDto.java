package com.sigi.presentation.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewUserRequest", description = "Payload for creating a new user account")
public class NewUserDto {

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(
            description = "Password for the new user. Minimum 8 characters, should include letters, numbers and symbols.",
            example = "SecurePass2025!",
            minLength = 8
    )
    private String password;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid format")
    @Schema(
            description = "Email address of the new user. Must be unique and valid.",
            example = "newuser@example.com",
            format = "email"
    )
    private String email;

    @NotBlank(message = "Name must not be blank")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-záéíóúÁÉÍÓÚüÜñÑ\\s-]+$", message = "Name contains invalid characters")
    @Schema(
            description = "First name of the user. Only letters, spaces and hyphens allowed.",
            example = "Carlos",
            minLength = 3,
            maxLength = 50
    )
    private String name;

    @NotBlank(message = "Surname must not be blank")
    @Size(min = 5, max = 50, message = "Surname must be between 5 and 50 characters")
    @Pattern(regexp = "^[A-Za-záéíóúÁÉÍÓÚüÜñÑ\\s-]+$", message = "Surname contains invalid characters")
    @Schema(
            description = "Surname of the user. Only letters, spaces and hyphens allowed.",
            example = "González Murillo",
            minLength = 5,
            maxLength = 50
    )
    private String surname;

    @NotBlank(message = "Phone number must not be blank")
    @Size(min = 7, max = 15, message = "Phone number must be between 7 and 15 characters")
    @Pattern(
            regexp = "^\\+?[0-9]{1,4}?[\\s\\-]?[0-9]{1,3}([\\s\\-]?[0-9]{3,4}){2}$",
            message = "Phone number must be a valid international format"
    )
    @Schema(
            description = "Phone number of the user in international format. Must be unique.",
            example = "+573001234567",
            minLength = 7,
            maxLength = 15
    )
    private String phoneNumber;

    @Schema(description = "Role assigned to the user. Must be one of the predefined roles in the system.",
            example = "ROLE_ADMIN")
    private String role;
}


