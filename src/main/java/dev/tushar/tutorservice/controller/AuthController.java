package dev.tushar.tutorservice.controller;

import dev.tushar.tutorservice.dto.ApiResponse;
import dev.tushar.tutorservice.dto.request.AuthRequestDTO;
import dev.tushar.tutorservice.dto.request.RefreshTokenRequestDTO;
import dev.tushar.tutorservice.dto.request.RegisterRequestDTO;
import dev.tushar.tutorservice.dto.response.LoginResponseDTO;
import dev.tushar.tutorservice.dto.response.RefreshTokenResponseDTO;
import dev.tushar.tutorservice.dto.response.RegisterResponseDTO;
import dev.tushar.tutorservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> register(
            @RequestBody @Valid RegisterRequestDTO request) {
        RegisterResponseDTO registeredUser = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Registration successful",
                        registeredUser,
                        HttpStatus.CREATED.value()),
                HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> authenticate(
            @RequestBody @Valid AuthRequestDTO request,
            HttpServletRequest httpServletRequest) {
        String ipAddress = httpServletRequest.getRemoteAddr();
        String deviceInfo = httpServletRequest.getHeader("User-Agent");

        LoginResponseDTO loginResponse = authService.authenticate(request, ipAddress, deviceInfo);
        if (loginResponse.activeSessions() != null) {
            return new ResponseEntity<>(
                    ApiResponse.error(
                            "Maximum number of active sessions reached.",
                            loginResponse,
                            HttpStatus.CONFLICT.value()),
                    HttpStatus.CONFLICT);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authentication successful",
                        loginResponse,
                        HttpStatus.OK.value()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        RefreshTokenResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tokens refreshed successfully.",
                        response,
                        HttpStatus.OK.value()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String accessToken = authHeader.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logout successful.",
                        null,
                        HttpStatus.OK.value()));
    }
}
