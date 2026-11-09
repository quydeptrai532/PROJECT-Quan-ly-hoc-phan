package com.edu.registration.exception;

import com.edu.registration.dto.ValidationErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ValidationErrorDetail> errors;
    private final Long currentVersion;
    private final Integer currentPhienBanBangDiem;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of(), null, null);
    }

    public ApiException(HttpStatus status, String message, List<ValidationErrorDetail> errors) {
        this(status, message, errors, null, null);
    }

    public ApiException(HttpStatus status, String message, List<ValidationErrorDetail> errors,
                        Long currentVersion, Integer currentPhienBanBangDiem) {
        super(message);
        this.status = status;
        this.errors = errors != null ? errors : List.of();
        this.currentVersion = currentVersion;
        this.currentPhienBanBangDiem = currentPhienBanBangDiem;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<ValidationErrorDetail> getErrors() {
        return errors;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }

    public Integer getCurrentPhienBanBangDiem() {
        return currentPhienBanBangDiem;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException gradeConflict(String message, Long currentVersion, Integer currentPhienBanBangDiem) {
        return new ApiException(HttpStatus.CONFLICT, message, List.of(), currentVersion, currentPhienBanBangDiem);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException validation(String message, List<ValidationErrorDetail> errors) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, errors);
    }
}
