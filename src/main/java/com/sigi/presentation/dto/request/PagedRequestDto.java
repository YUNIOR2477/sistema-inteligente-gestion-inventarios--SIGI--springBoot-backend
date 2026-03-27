package com.sigi.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import static com.sigi.util.Constants.SIZE_PAGE_DEFAULT;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "PagedRequestDto ", description = "Payload for creating or updating a product")
public class PagedRequestDto  {
    @NotNull(message = "Page number is required")
    @Min(value = 0, message = "Page must be zero or positive")
    private Integer page = 0;

    @NotNull(message = "Page size is required")
    @Min(value = 1, message = "Size must be at least 1")
    private Integer size = SIZE_PAGE_DEFAULT;

    @Size(max = 64, message = "Sort field must not exceed 64 characters")
    private String sortField = "createdAt";

    private String sortDirection = "desc";

    @Size(max = 128, message = "Search value must not exceed 128 characters")
    private String searchValue;

    private UUID searchId;

    private BigDecimal searchNumber;
}
