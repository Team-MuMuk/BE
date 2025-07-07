package com.mumuk.global.apiPayload.exception;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.BaseResponse;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * Constructs an AuthException with the specified error code and its associated message.
     *
     * @param errorCode the error code representing the authentication error
     */
    public AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Converts this authentication exception into a standardized failure response.
     *
     * @return a {@code BaseResponse<String>} containing failure status, error code, and error message
     */
    public BaseResponse<String> toResponse() {
        return new BaseResponse<>(false, errorCode.getCode(), errorCode.getMessage());
    }
}
