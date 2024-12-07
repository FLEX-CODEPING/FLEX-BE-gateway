package codeping.flex.gateway.global.common.exception;

import codeping.flex.gateway.global.common.response.ApplicationResponse;
import codeping.flex.gateway.global.common.response.code.BaseErrorCode;
import codeping.flex.gateway.global.common.response.code.CommonErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ApplicationResponse<Object>>> handleValidationException(ConstraintViolationException e, ServerWebExchange exchange) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ConstraintViolationException Error"));

        return createErrorResponse(CommonErrorCode.valueOf(errorMessage), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApplicationResponse<Object>>> handleGenericException(Exception e, ServerWebExchange exchange) {
        log.error("Unexpected error occurred", e);
        return createErrorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, exchange, e.getMessage());
    }

    @ExceptionHandler(ApplicationException.class)
    public Mono<ResponseEntity<ApplicationResponse<Object>>> handleApplicationException(ApplicationException e, ServerWebExchange exchange) {
        BaseErrorCode errorCode = e.getCode();
        return createErrorResponse(errorCode, exchange);
    }

    private Mono<ResponseEntity<ApplicationResponse<Object>>> createErrorResponse(BaseErrorCode errorCode, ServerWebExchange exchange) {
        return createErrorResponse(errorCode, exchange, null);
    }

    private Mono<ResponseEntity<ApplicationResponse<Object>>> createErrorResponse(BaseErrorCode errorCode, ServerWebExchange exchange, Object errorDetails) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
                errorCode.getCustomCode(),
                errorCode.getMessage(),
                errorDetails
        );

        return Mono.just(ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(body));
    }

    private Mono<ResponseEntity<ApplicationResponse<Object>>> createErrorResponse(BaseErrorCode errorCode, ServerWebExchange exchange, Map<String, String> errorArgs) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
                errorCode.getCustomCode(),
                errorCode.getMessage(),
                errorArgs
        );

        return Mono.just(ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(body));
    }
}
