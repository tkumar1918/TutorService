package dev.tushar.tutorservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status", "code", "message", "data", "error"
})
public class ApiResponse<T> {

    boolean status;
    int code;
    String message;
    T data;
    T error;

    // --- Success Response ---
    public static <T> ApiResponse<T> success(String message, T data, int code) {
        return ApiResponse.<T>builder()
                .status(true)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    // --- Error Response ---
    public static <T> ApiResponse<T> error(String message, T error, int code) {
        return ApiResponse.<T>builder()
                .status(false)
                .code(code)
                .message(message)
                .error(error)
                .build();
    }
}
