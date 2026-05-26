package com.tsolmon.online_teaching_platform.common.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return validationResponse(ex.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex) {
        return validationResponse(ex.getBindingResult());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiFieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", "Validation failed", fieldErrors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reason = ex.getReason();
        String code = resolveErrorCode(status, reason);
        String message = reason == null || reason.isBlank()
                ? (status == null ? "Request failed" : status.getReasonPhrase())
                : reason;
        return ResponseEntity.status(statusCode).body(ApiErrorResponse.of(code, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Request violates a data constraint"));
    }

    private ResponseEntity<ApiErrorResponse> validationResponse(BindingResult bindingResult) {
        List<ApiFieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", "Validation failed", fieldErrors));
    }

    private ApiFieldError toFieldError(FieldError error) {
        return new ApiFieldError(
                error.getField(),
                resolveValidationCode(error.getField(), error.getCode()),
                error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
        );
    }

    private ApiFieldError toFieldError(ConstraintViolation<?> violation) {
        String field = extractFieldName(violation);
        return new ApiFieldError(
                field,
                resolveValidationCode(field, violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()),
                violation.getMessage()
        );
    }

    private static String extractFieldName(ConstraintViolation<?> violation) {
        String field = "";
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getName() != null && !node.getName().isBlank()) {
                field = node.getName();
            }
        }
        return field;
    }

    private static String resolveValidationCode(String field, String validationRule) {
        if (validationRule == null || validationRule.isBlank()) {
            return "INVALID";
        }
        return switch (validationRule) {
            case "NotBlank", "NotNull" -> "REQUIRED";
            case "Email" -> "INVALID_EMAIL";
            case "Size" -> "password".equals(field) ? "PASSWORD_LENGTH" : "INVALID_LENGTH";
            default -> validationRule.toUpperCase(Locale.ROOT);
        };
    }

    private static String resolveErrorCode(HttpStatus status, String reason) {
        if (reason != null) {
            return switch (reason) {
                case "Email already registered" -> "EMAIL_ALREADY_REGISTERED";
                case "Invalid email or password" -> "INVALID_CREDENTIALS";
                case "Teacher profile not found" -> "TEACHER_PROFILE_NOT_FOUND";
                case "Teacher not found" -> "TEACHER_NOT_FOUND";
                case "User not found" -> "USER_NOT_FOUND";
                case "File is required" -> "FILE_REQUIRED";
                case "An image file is required" -> "IMAGE_FILE_REQUIRED";
                case "Image too large (max 5MB)" -> "IMAGE_TOO_LARGE";
                case "Image upload is not configured (set CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET)" ->
                        "IMAGE_UPLOAD_NOT_CONFIGURED";
                case "Upload did not return a URL" -> "IMAGE_UPLOAD_NO_URL";
                case "Could not upload image" -> "IMAGE_UPLOAD_FAILED";
                default -> defaultStatusCode(status);
            };
        }
        return defaultStatusCode(status);
    }

    private static String defaultStatusCode(HttpStatus status) {
        if (status == null) {
            return "REQUEST_FAILED";
        }
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case PAYLOAD_TOO_LARGE -> "PAYLOAD_TOO_LARGE";
            case BAD_GATEWAY -> "BAD_GATEWAY";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> "HTTP_" + status.value();
        };
    }
}
