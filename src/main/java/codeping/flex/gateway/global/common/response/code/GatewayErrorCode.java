package codeping.flex.gateway.global.common.response.code;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GatewayErrorCode implements BaseErrorCode{
    JWT_EXPIRED(HttpStatus.BAD_REQUEST, "JWT_003", "만료된 토큰입니다."),
    INVALID_JWT(HttpStatus.BAD_REQUEST, "JWT_005", "유효하지 않은 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.BAD_REQUEST, "JWT_006","accees token이 비어있습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    @Override
    public String getCustomCode() {
        return this.customCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
