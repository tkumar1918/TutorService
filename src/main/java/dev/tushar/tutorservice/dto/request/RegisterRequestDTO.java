package dev.tushar.tutorservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO {

    @Length(min = 3, max = 50, message = "First name must be between 3 and 50 characters long")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Length(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;

    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "^.{4,}$", message = "Password must be at least 4 characters long")
    @NotBlank(message = "Password is required")
    private String password;
}
