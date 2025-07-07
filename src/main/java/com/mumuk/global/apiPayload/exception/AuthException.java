package com.mumuk.global.apiPayload.exception;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.BaseResponse;
import com.mumuk.global.apiPayload.response.Response;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public Response<Void> toResponse() {
        return  Response.fail(errorCode);
    }
}
