package com.sigi.presentation.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "LoginUserRequest", description = "Payload for user login containing credentials")
public class LoginUserDto {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid format")
    @Schema(
            description = "Email address used for login. Must be a valid email format.",
            example = "user@example.com",
            format = "email"
    )
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(
            description = "Password for login. Minimum 8 characters, should include letters, numbers and symbols.",
            example = "StrongPass123!",
            minLength = 8
    )
    private String password;
}


