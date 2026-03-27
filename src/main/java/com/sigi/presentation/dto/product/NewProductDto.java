package com.sigi.presentation.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewProductRequest", description = "Payload for creating or updating a product")
public class NewProductDto {

    @NotBlank(message = "SKU must not be empty.")
    @Size(max = 50, message = "SKU must not exceed 50 characters.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "SKU may only contain letters, numbers, hyphens, and underscores.")
    @Schema(description = "Stock Keeping Unit (SKU) code. Must be unique.",
            example = "SKU-12345",
            maxLength = 50)
    private String sku;

    @NotBlank(message = "Product name must not be empty.")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters.")
    @Schema(description = "Name of the product.",
            example = "Laptop Lenovo ThinkPad",
            minLength = 3,
            maxLength = 100)
    private String name;

    @NotBlank(message = "Category must not be empty.")
    @Size(min = 3, max = 50, message = "Category must be between 3 and 50 characters.")
    @Schema(description = "Category to which the product belongs.",
            example = "Electronics",
            minLength = 3,
            maxLength = 50)
    private String category;

    @NotNull(message = "Unit must not be null.")
    @Schema(description = "Unit of measurement for the product (e.g., piece, kg, liter).",
            example = "10")
    private String unit;

    @NotNull(message = "Price must not be null.")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than 0.")
    @Digits(integer = 10, fraction = 2, message = "Price must be a valid monetary amount with up to 2 decimals.")
    @Schema(description = "Unit price of the product.",
            example = "2500.99",
            type = "number",
            format = "decimal",
            minimum = "0.01")
    private BigDecimal price;

    @Size(max = 30, message = "Barcode must not exceed 30 characters.")
    @Pattern(regexp = "^[0-9]+$", message = "Barcode must contain only digits.")
    @Schema(description = "Barcode of the product. Digits only.",
            example = "7701234567890",
            maxLength = 30,
            nullable = true)
    private String barcode;

    @Size(max = 255, message = "Image URL must not exceed 255 characters.")
    @Pattern(regexp = "^(http|https)://.*$", message = "Image URL must be a valid URL starting with http or https.")
    @Schema(description = "Image URL of the product.",
            example = "https://example.com/images/product123.jpg",
            format = "uri",
            maxLength = 255,
            nullable = true)
    private String imageUrl;
}

