package com.sigi.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(name = "PagedResponse", description = "Pagination wrapper containing content and metadata about the page")
public class PagedResponse<T> {

    @Schema(description = "List of items in the current page.",
            example = "[{...}, {...}]")
    private List<T> content;

    @Schema(description = "Current page number (0-based).",
            example = "0")
    private int pageNumber;

    @Schema(description = "Size of the page (number of items per page).",
            example = "10")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages.",
            example = "125")
    private long totalElements;

    @Schema(description = "Total number of pages.",
            example = "13")
    private int totalPages;

    @Schema(description = "Indicates whether this is the last page.",
            example = "false")
    private boolean last;
}
