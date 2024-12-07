package codeping.flex.gateway.global.common.response;

import codeping.flex.gateway.global.common.response.code.SuccessStatusCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApplicationResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public static <T> ApplicationResponse<T> onSuccess(T result) {
        return new ApplicationResponse<>(
                true,
                SuccessStatusCode.SUCCESS.getCustomCode(),
                SuccessStatusCode.SUCCESS.getMessage(),
                result
        );
    }

    public static <T> ApplicationResponse<T> onFailure(String code, String message, T result) {
        return new ApplicationResponse<>(
                false,
                code,
                message,
                result
        );
    }
}