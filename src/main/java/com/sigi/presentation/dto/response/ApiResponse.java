package com.sigi.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "data", "errors", "timestamp", "traceId", "version"})
@Schema(name = "ApiResponse", description = "Standard API response wrapper containing metadata and payload")
public class ApiResponse<T> {

    @Schema(description = "HTTP status code of the response.",
            example = "200")
    private int code;

    @Schema(description = "Message describing the result of the operation.",
            example = "Operation completed successfully")
    private String message;

    @Schema(description = "Payload data returned by the API. Type varies depending on the endpoint.",
            nullable = true)
    private T data;

    @Schema(description = "List of error messages, if any.",
            example = "[\"Invalid request parameter\"]",
            nullable = true)
    private List<String> errors;

    @Builder.Default
    @Schema(description = "Timestamp when the response was generated (ISO 8601).",
            example = "2026-01-10T17:38:00-05:00",
            format = "date-time")
    private String timestamp = OffsetDateTime.now(ZoneId.of("America/Bogota")).toString();

    @Builder.Default
    @Schema(description = "Correlation trace ID for tracking requests across logs.",
            example = "c1234567-89ab-4def-0123-456789abcdef",
            nullable = true)
    private String traceId = MDC.get("correlationId");

    @Builder.Default
    @Schema(description = "Version of the API response format.",
            example = "1.0")
    private String version = "1.0";


    public static <T> ApiResponse<T> success(String successMessage, T data) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .message(successMessage)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .build();
    }

    public static <T> ApiResponse<PagedResponse<T>> successPage(String successMessage, Page<T> page) {
        PagedResponse<T> pagedResponse = PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();

        return ApiResponse.<PagedResponse<T>>builder()
                .code(HttpStatus.OK.value())
                .message(successMessage)
                .data(pagedResponse)
                .build();
    }
}
