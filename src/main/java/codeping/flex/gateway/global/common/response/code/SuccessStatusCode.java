package codeping.flex.gateway.global.common.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatusCode {

    SUCCESS(HttpStatus.OK, "SUCCESS", "성공"),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;

    public int getHttpStatus() {
        return httpStatus.value();
    }
}
