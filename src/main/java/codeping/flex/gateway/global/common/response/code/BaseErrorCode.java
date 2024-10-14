package codeping.flex.gateway.global.common.response.code;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCustomCode();
    String getMessage();
}
