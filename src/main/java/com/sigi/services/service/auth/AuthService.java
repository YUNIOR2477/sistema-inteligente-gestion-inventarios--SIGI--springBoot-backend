package com.sigi.services.service.auth;

import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.UserRepository;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.auth.LoginUserDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.sigi.util.Constants.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserService authUserService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public ApiResponse<String> authenticate(LoginUserDto loginDto, HttpServletRequest request) {
        log.debug("(authenticate) -> " + PERFORMING_OPERATION);
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword());
            Authentication authResult = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authResult);
            User authUser = userRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new EntityNotFoundException(EMAIL_NOT_FOUND.formatted(loginDto.getEmail())));
            authUser.setLastLogin(LocalDateTime.now(ZoneId.of("America/Bogota")));
            authUserService.save(authUser);
            log.info("(authenticate) -> " + OPERATION_COMPLETED);
            return ApiResponse.success(SUCCESSFUL_LOGIN, jwtUtil.generateAccessToken(authResult, request));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
    }
}

