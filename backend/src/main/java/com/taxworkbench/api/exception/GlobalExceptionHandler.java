package com.taxworkbench.api.exception;

import com.taxworkbench.api.dto.workitem.ConcurrencyConflictResponse;
import com.taxworkbench.api.observability.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/not-found"));
        problem.setTitle("Resource Not Found");
        attachObservability(problem, "NOT_FOUND", request);
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(Exception ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "https://api.taxworkbench.dev/errors/not-found",
                "Resource Not Found",
                "NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/validation"));
        problem.setTitle("Bad Request");

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        attachObservability(problem, "VALIDATION_ERROR", request);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/constraint-violation"));
        problem.setTitle("Bad Request");
        attachObservability(problem, "CONSTRAINT_VIOLATION", request);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return switch (ex.getMessage()) {
            case "INVALID_CREDENTIALS" -> buildProblem(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid id or password",
                    "https://api.taxworkbench.dev/errors/invalid-credentials",
                    "Unauthorized",
                    "INVALID_CREDENTIALS",
                    request
            );
            case "UNAUTHORIZED" -> buildProblem(
                    HttpStatus.UNAUTHORIZED,
                    "Login required",
                    "https://api.taxworkbench.dev/errors/unauthorized",
                    "Unauthorized",
                    "UNAUTHORIZED",
                    request
            );
            case "ACCOUNT_DISABLED" -> buildProblem(
                    HttpStatus.FORBIDDEN,
                    "Account is disabled",
                    "https://api.taxworkbench.dev/errors/account-disabled",
                    "Forbidden",
                    "ACCOUNT_DISABLED",
                    request
            );
            case "USER_ID_ALREADY_EXISTS" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "User id already exists",
                    "https://api.taxworkbench.dev/errors/user-id-conflict",
                    "Conflict",
                    "USER_ID_ALREADY_EXISTS",
                    request
            );
            case "MENU_ID_ALREADY_EXISTS" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Menu id already exists",
                    "https://api.taxworkbench.dev/errors/menu-id-conflict",
                    "Conflict",
                    "MENU_ID_ALREADY_EXISTS",
                    request
            );
            case "MENU_HAS_CHILDREN" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Cannot delete menu because child menus exist",
                    "https://api.taxworkbench.dev/errors/menu-has-children",
                    "Conflict",
                    "MENU_HAS_CHILDREN",
                    request
            );
            case "PARENT_MENU_NOT_FOUND" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "Parent menu not found",
                    "https://api.taxworkbench.dev/errors/parent-menu-not-found",
                    "Bad Request",
                    "PARENT_MENU_NOT_FOUND",
                    request
            );
            case "INVALID_PARENT_MENU" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "Invalid parent menu relation",
                    "https://api.taxworkbench.dev/errors/invalid-parent-menu",
                    "Bad Request",
                    "INVALID_PARENT_MENU",
                    request
            );
            case "CLIENT_HAS_WORK_ITEMS" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Cannot delete client because work items exist",
                    "https://api.taxworkbench.dev/errors/client-has-work-items",
                    "Conflict",
                    "CLIENT_HAS_WORK_ITEMS",
                    request
            );
            case "CLIENT_TYPE_CONFLICT_WITH_WORK_ITEMS" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Cannot change client type because incompatible work items exist",
                    "https://api.taxworkbench.dev/errors/client-type-conflict-with-work-items",
                    "Conflict",
                    "CLIENT_TYPE_CONFLICT_WITH_WORK_ITEMS",
                    request
            );
            case "CLIENT_INACTIVE" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Cannot create work item for inactive client",
                    "https://api.taxworkbench.dev/errors/client-inactive",
                    "Conflict",
                    "CLIENT_INACTIVE",
                    request
            );
            case "INVALID_WORK_TYPE_FOR_CLIENT_TYPE" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "Invalid work type for client type",
                    "https://api.taxworkbench.dev/errors/invalid-work-type-for-client-type",
                    "Bad Request",
                    "INVALID_WORK_TYPE_FOR_CLIENT_TYPE",
                    request
            );
            case "VIP_DUE_DATE_EXCEEDED" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "VIP client due date must be within 14 days",
                    "https://api.taxworkbench.dev/errors/vip-due-date-exceeded",
                    "Bad Request",
                    "VIP_DUE_DATE_EXCEEDED",
                    request
            );
            case "CLIENT_INACTIVE_ONLY_HOLD_ALLOWED" -> buildProblem(
                    HttpStatus.CONFLICT,
                    "Inactive client work item status can only be HOLD",
                    "https://api.taxworkbench.dev/errors/client-inactive-only-hold-allowed",
                    "Conflict",
                    "CLIENT_INACTIVE_ONLY_HOLD_ALLOWED",
                    request
            );
            case "CSV_EMPTY_FILE" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "CSV file is empty",
                    "https://api.taxworkbench.dev/errors/csv-empty-file",
                    "Bad Request",
                    "CSV_EMPTY_FILE",
                    request
            );
            case "CSV_INVALID_HEADER" -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    "Invalid CSV header. Expected: clientId,type,assignee,dueDate,tags,memo",
                    "https://api.taxworkbench.dev/errors/csv-invalid-header",
                    "Bad Request",
                    "CSV_INVALID_HEADER",
                    request
            );
            default -> buildProblem(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    "https://api.taxworkbench.dev/errors/bad-request",
                    "Bad Request",
                    "BAD_REQUEST",
                    request
            );
        };
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        if ("DOWNLOAD_LIMIT_EXCEEDED".equals(ex.getMessage())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Download payload exceeded configured limit"
            );
            problem.setType(URI.create("https://api.taxworkbench.dev/errors/download-limit"));
            problem.setTitle("Payload Too Large");
            attachObservability(problem, "DOWNLOAD_LIMIT_EXCEEDED", request);
            return problem;
        }
        if ("BULK_FILE_UPLOAD_FAILED".equals(ex.getMessage())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Bulk CSV upload failed while staging the uploaded file"
            );
            problem.setType(URI.create("https://api.taxworkbench.dev/errors/bulk-file-upload-failed"));
            problem.setTitle("Internal Server Error");
            attachObservability(problem, "BULK_FILE_UPLOAD_FAILED", request);
            return problem;
        }
        return handleUnknown(ex, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeded configured size limit"
        );
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/upload-size-limit"));
        problem.setTitle("Payload Too Large");
        attachObservability(problem, "UPLOAD_SIZE_LIMIT_EXCEEDED", request);
        return problem;
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid multipart upload request"
        );
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/multipart-invalid"));
        problem.setTitle("Bad Request");
        attachObservability(problem, "MULTIPART_REQUEST_INVALID", request);
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingRequestPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required multipart part: " + ex.getRequestPartName()
        );
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/multipart-part-missing"));
        problem.setTitle("Bad Request");
        attachObservability(problem, "MULTIPART_PART_MISSING", request);
        return problem;
    }

    @ExceptionHandler(ConcurrencyConflictException.class)
    public org.springframework.http.ResponseEntity<ConcurrencyConflictResponse> handleConflict(ConcurrencyConflictException ex) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConcurrencyConflictResponse(
                        "CONCURRENT_MODIFICATION",
                        ex.getMessage(),
                        ex.getClientVersion(),
                        ex.getServerVersion(),
                        ex.getServerSnapshot()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error"
        );
        problem.setType(URI.create("https://api.taxworkbench.dev/errors/internal"));
        problem.setTitle("Internal Server Error");
        attachObservability(problem, "INTERNAL_ERROR", request);
        return problem;
    }

    private void attachObservability(ProblemDetail problem, String code, HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        if (requestId != null) {
            problem.setProperty("requestId", String.valueOf(requestId));
        }
        problem.setProperty("code", code);
    }

    private ProblemDetail buildProblem(
            HttpStatus status,
            String detail,
            String type,
            String title,
            String code,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        attachObservability(problem, code, request);
        return problem;
    }
}

