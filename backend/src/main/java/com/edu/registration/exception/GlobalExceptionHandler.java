package com.edu.registration.exception;

import com.edu.registration.dto.ValidationErrorDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern INDEXED_FIELD_PATTERN = Pattern.compile(".*?\\[(\\d+)\\]\\.(.+)");

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ErrorResponse(
            String message,
            Instant timestamp,
            List<ValidationErrorDetail> errors,
            Long currentVersion,
            Integer currentPhienBanBangDiem) {

        public ErrorResponse(String message, Instant timestamp) {
            this(message, timestamp, List.of(), null, null);
        }

        public ErrorResponse(String message, Instant timestamp, List<ValidationErrorDetail> errors) {
            this(message, timestamp, errors, null, null);
        }
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(
                        ex.getMessage(),
                        Instant.now(),
                        ex.getErrors(),
                        ex.getCurrentVersion(),
                        ex.getCurrentPhienBanBangDiem()));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex) {
        log.warn("Xung đột cập nhật đồng thời (Optimistic Locking): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang để đối chiếu phiên bản mới nhất.",
                        Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationErrorDetail> details = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String fieldPath = fe.getField();
            Integer dong = null;
            String cot = fieldPath;
            Matcher matcher = INDEXED_FIELD_PATTERN.matcher(fieldPath);
            if (matcher.matches()) {
                try {
                    dong = Integer.parseInt(matcher.group(1)) + 1;
                    cot = matcher.group(2);
                } catch (NumberFormatException ignored) {
                }
            }
            details.add(new ValidationErrorDetail(
                    null, null, null, dong, cot, fieldPath, fe.getDefaultMessage()));
        }
        String firstMessage = details.isEmpty() ? "Dữ liệu không hợp lệ" : details.get(0).message();
        return ResponseEntity.badRequest().body(new ErrorResponse(firstMessage, Instant.now(), details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu gửi lên không hợp lệ", Instant.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Dữ liệu đang được tham chiếu hoặc vi phạm ràng buộc duy nhất", Instant.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Kích thước tệp tải lên vượt quá giới hạn cho phép", Instant.now()));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Yêu cầu tải tệp lên không hợp lệ", Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Bạn không có quyền thực hiện thao tác này", Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Lỗi hệ thống chưa được xử lý", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Đã xảy ra lỗi hệ thống", Instant.now()));
    }
}
