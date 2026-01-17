package dev.tushar.tutorservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {

    @Email(message = "Please provide a valid email address")
    @NotBlank
    private String email;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\s\\W]).{6,}$", message = "Password must be at least 6 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    @NotBlank
    private String password;

    // This field is optional.
    // This is used to invalidate the existing session,
    // when user have exceeded the maximum number of allowed sessions.
    private UUID sessionIdToInvalidate;
}
