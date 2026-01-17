package dev.tushar.tutorservice.dto.response;

public record RegisterResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email) {
}
