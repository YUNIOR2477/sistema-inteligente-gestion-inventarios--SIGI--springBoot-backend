package com.sigi.presentation.dto.dispatcher;

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
@Schema(name = "NewDispatcherRequest", description = "Payload for creating or updating a dispatcher")
public class NewDispatcherDto {

    @NotBlank(message = "Name must not be empty.")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    @Schema(description = "Full name of the dispatcher. Only letters, spaces, apostrophes and hyphens allowed.",
            example = "Juan Pérez",
            minLength = 2,
            maxLength = 100)
    private String name;

    @NotBlank(message = "Identification must not be empty.")
    @Size(min = 5, max = 20, message = "Identification must be between 5 and 20 characters.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Identification may contain only letters, numbers and hyphens.")
    @Schema(description = "Identification code for the dispatcher. Normalized (no punctuation).",
            example = "DISP-10234",
            minLength = 5,
            maxLength = 20)
    private String identification;

    @Size(max = 255, message = "Location must not exceed 255 characters.")
    @Schema(description = "Human readable location or address of the dispatcher.",
            example = "Calle 10 # 5-20, Armenia, Quindío",
            maxLength = 255,
            nullable = true)
    private String location;

    @Size(max = 20, message = "Phone must not exceed 20 characters.")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid international number in E.164 format (e.g. +573001234567).")
    @Schema(description = "Contact phone number in E.164 format.",
            example = "+573001234567",
            pattern = "^\\+?[1-9]\\d{1,14}$",
            maxLength = 20,
            nullable = true)
    private String phone;

    @Size(max = 254, message = "Email must not exceed 254 characters.")
    @Email(message = "Email must be a valid email address.")
    @Schema(description = "Contact email address of the dispatcher.",
            example = "dispatcher@example.com",
            format = "email",
            maxLength = 254,
            nullable = true)
    private String email;

    @NotBlank(message = "Contact must not be empty.")
    @Schema(description = "Primary contact person or reference for the dispatcher.",
            example = "Carlos Gómez")
    private String contact;
}


