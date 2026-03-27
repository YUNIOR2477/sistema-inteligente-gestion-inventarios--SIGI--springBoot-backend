package com.sigi.presentation.dto.client;

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
@Schema(name = "ClientRequest", description = "payload to create or update customer data")
public class NewClientDto {

    @NotBlank(message = "Name must not be empty.")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    @Schema(description = "Full name of the client. Use canonical display name.",
            example = "María Fernanda Gómez",
            maxLength = 100)
    private String name;

    @NotBlank(message = "Identification must not be empty.")
    @Size(min = 5, max = 30, message = "Identification must be between 5 and 30 characters.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Identification may contain only letters, numbers and hyphens.")
    @Schema(description = "National or business identification number, normalized (no punctuation).",
            example = "1023456789",
            maxLength = 30)
    private String identification;

    @NotBlank(message = "location must not be empty.")
    @Size(max = 255, message = "Location must not exceed 255 characters.")
    @Schema(description = "Human readable address or location. For structured addresses use a dedicated object.",
            example = "Calle 10 # 5-20, Armenia, Quindío",
            maxLength = 255)
    private String location;

    @Size(max = 20, message = "Phone must not exceed 20 characters.")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid international number in E.164 format (e.g. +573001234567).")
    @Schema(description = "Contact phone in E.164 format. Store normalized international format.",
            example = "+573001234567",
            pattern = "^\\+?[1-9]\\d{1,14}$",
            maxLength = 20)
    private String phone;

    @Size(max = 254, message = "Email must not exceed 254 characters.")
    @Email(message = "Email must be a valid email address.")
    @Schema(description = "Contact email address used for notifications or login.",
            example = "cliente@example.com",
            format = "email",
            maxLength = 254)
    private String email;
}
