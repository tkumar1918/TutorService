package dev.tushar.tutorservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object errorDetails;

    public ApiException(HttpStatus status, String message, Object errorDetails) {
        super(message);
        this.status = status;
        this.errorDetails = errorDetails;
    }

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }
}
