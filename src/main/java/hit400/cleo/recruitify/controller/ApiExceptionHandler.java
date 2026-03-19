package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.ApiErrorDto;
import hit400.cleo.recruitify.exception.ConflictException;
import hit400.cleo.recruitify.exception.NotFoundException;
import hit400.cleo.recruitify.service.leadiq.LeadIQApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> badRequest(IllegalArgumentException ex, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(LeadIQApiException.class)
    public ResponseEntity<ApiErrorDto> leadiqError(LeadIQApiException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus().value());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        String message = ex.getResponseBody() == null || ex.getResponseBody().isBlank() ? ex.getMessage() : ex.getResponseBody();
        return error(status, message, exchange);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> dataIntegrity(DataIntegrityViolationException ex, ServerWebExchange exchange) {
        return error(HttpStatus.CONFLICT, "Database constraint violation", exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiErrorDto> badInput(ServerWebInputException ex, ServerWebExchange exchange) {
        String message = ex.getReason() == null || ex.getReason().isBlank() ? "Invalid request body" : ex.getReason();
        return error(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorDto> notFound(NotFoundException ex, ServerWebExchange exchange) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorDto> conflict(ConflictException ex, ServerWebExchange exchange) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    private static ResponseEntity<ApiErrorDto> error(HttpStatus status, String message, ServerWebExchange exchange) {
        ApiErrorDto body = new ApiErrorDto(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange == null ? null : exchange.getRequest().getPath().value()
        );
        return ResponseEntity.status(status).body(body);
    }
}
