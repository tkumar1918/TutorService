package dev.tushar.tutorservice.dto.response;

import java.util.HashMap;

public record RefreshTokenResponseDTO(
        HashMap<String, Object> jwt,
        String refreshToken,
        Long refreshTokenExpiresIn) {
}
