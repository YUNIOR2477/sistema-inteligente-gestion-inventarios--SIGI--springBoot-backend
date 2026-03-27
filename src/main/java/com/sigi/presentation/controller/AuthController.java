package com.sigi.presentation.controller;

import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.services.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "auth", description = "Operations related to authentication and user registration")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token",
            operationId = "loginUser"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginUserDto loginDto, HttpServletRequest request) {

        log.info("(Login) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<String> jwt = authService.authenticate(loginDto, request);
        if (jwt.getCode() != HttpStatus.OK.value()) {
            return ResponseEntity.status(jwt.getCode()).body(jwt);
        }
        log.info("(Login) " + RESPONSE_SENT, HttpStatus.OK, SUCCESSFUL_LOGIN);
        return ResponseEntity.ok(jwt);
    }
}
