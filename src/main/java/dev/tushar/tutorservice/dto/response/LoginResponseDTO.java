package dev.tushar.tutorservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        Long id,
        String firstName,
        String email,
        HashMap<String, Object> jwt,
        String refreshToken,
        Long refreshTokenExpiresIn,
        List<Map<String, Object>> activeSessions) {
    // This is when user logged in successfully
    public LoginResponseDTO(Long id, String firstName, String email, HashMap<String, Object> jwt, String refreshToken,
            Long refreshTokenExpiresIn) {
        this(id, firstName, email, jwt, refreshToken, refreshTokenExpiresIn, null);
    }

    // And this one when user trying to login but have exceeded the
    // maximum number of active sessions
    public LoginResponseDTO(List<Map<String, Object>> activeSessions) {
        this(null, null, null, null, null, null, activeSessions);
    }
}
