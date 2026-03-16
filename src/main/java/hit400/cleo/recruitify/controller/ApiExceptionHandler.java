package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.ApiErrorDto;
import hit400.cleo.recruitify.service.apollo.ApolloApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> badRequest(IllegalArgumentException ex, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ApolloApiException.class)
    public ResponseEntity<ApiErrorDto> apolloError(ApolloApiException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus().value());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        String message = ex.getResponseBody() == null || ex.getResponseBody().isBlank() ? ex.getMessage() : ex.getResponseBody();
        return error(status, message, exchange);
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
