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

    public static <T> Response<T> ok(T data) {

        return new Response<>(ResultCode.OK.getStatus(), ResultCode.OK.getCode(), ResultCode.OK.getMessage(), data);
    }

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
