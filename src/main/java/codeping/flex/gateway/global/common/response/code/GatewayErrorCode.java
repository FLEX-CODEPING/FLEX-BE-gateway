package codeping.flex.gateway.global.common.response.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GatewayErrorCode implements BaseErrorCode{
    EMPTY_TOKEN(HttpStatus.FORBIDDEN, "AUTHORIAZTION_001","accees token이 비어있습니다."),
    INVALID_TOKEN(HttpStatus.FORBIDDEN, "AUTHORIAZTION_002","유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.FORBIDDEN, "AUTHORIAZTION_003","사용기간이 만료된 토큰입니다."),

    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() {
        return null;
    }

    @Override
    public String getCustomCode() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }
}
