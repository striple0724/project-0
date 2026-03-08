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
        return handleUnknown(ex, request);
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
