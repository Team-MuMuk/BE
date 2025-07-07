package com.mumuk.global.apiPayload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mumuk.global.apiPayload.code.BaseCode;
import com.mumuk.global.apiPayload.code.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "code", "message", "data"})
public class Response<T> {

    private final HttpStatus status;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static Response<Void> ok() {

        return new Response<>(ResultCode.OK.getStatus(), ResultCode.OK.getCode(), ResultCode.OK.getMessage(), null);
    }

    /**
     * Creates a successful API response with the default success code and message, including the provided data.
     *
     * @param data the response payload to include
     * @return a Response containing the default success status, code, message, and the specified data
     */
    public static <T> Response<T> ok(T data) {

        return new Response<>(ResultCode.OK.getStatus(), ResultCode.OK.getCode(), ResultCode.OK.getMessage(), data);
    }


    /**
     * Creates a successful API response using the specified {@link ResultCode} and data.
     *
     * @param resultCode the result code containing status, code, and message for the response
     * @param data the payload to include in the response
     * @return a {@code Response} object representing a successful operation with the provided result code and data
     */
    public static <T> Response<T> ok(ResultCode resultCode, T data) {
        return new Response<>(resultCode.getStatus(), resultCode.getCode(), resultCode.getMessage(), data);
    }

    /**
     * Creates a new Response instance using the provided BaseCode and data.
     *
     * @param code the code object containing status, code, and message for the response
     * @param data the response payload to include
     * @return a Response object with the specified status, code, message, and data
     */
    public static <T> Response<T> of(BaseCode code, T data){

        return new Response<>(code.getStatus(), code.getCode(), code.getMessage(), data);
    }

    public static <T> Response<T> fail(BaseCode code) {

        return new Response<>(code.getStatus(), code.getCode(), code.getMessage(), null);
    }

    public static <T> Response<T> fail(BaseCode code, T data) {

        return new Response<>(code.getStatus(), code.getCode(), code.getMessage(), data);
    }

}
