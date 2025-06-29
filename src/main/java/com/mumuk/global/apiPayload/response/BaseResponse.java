package com.mumuk.global.apiPayload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> BaseResponse<T> onSuccess(T data) {
        return new BaseResponse<>(true, "success", data);
    }
}
