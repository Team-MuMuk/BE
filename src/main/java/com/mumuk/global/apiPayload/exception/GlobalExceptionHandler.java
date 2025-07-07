package com.mumuk.global.apiPayload.exception;


import com.mumuk.global.apiPayload.code.BaseCode;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *  Exception 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ===================== 사용자 정의 예외 ======================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(Response.fail(ex.getCode()));
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<Response<Void>> handleGlobalException(GlobalException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(Response.fail(ex.getCode()));
    }

    // ===================== Validation / 변환 오류 ======================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Response.fail(ErrorCode.BAD_REQUEST, errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Response.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            DataIntegrityViolationException.class,
            IllegalArgumentException.class,
            IncorrectResultSizeDataAccessException.class,
            InvalidDataAccessApiUsageException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<Object> handleBadRequestExceptions(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Response.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    // ===================== 시스템 오류 (500) ======================

    @ExceptionHandler({
            BeanCreationException.class,
            ClassCastException.class,
            HttpMessageConversionException.class,
            JpaSystemException.class,
            NullPointerException.class,
            UnsatisfiedDependencyException.class
    })
    public ResponseEntity<Object> handleServerExceptions(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    /**
     * Handles any unhandled exceptions by returning a generic internal server error response.
     *
     * Logs the exception and responds with HTTP 500 and a standardized error code, without exposing internal details.
     *
     * @return a ResponseEntity containing a failure response with an internal server error code
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }


    /**
     * Handles authentication-related exceptions and returns a standardized error response.
     *
     * Logs the authentication error and responds with the appropriate HTTP status and error code.
     *
     * @param ex the authentication exception to handle
     * @return a response entity containing the failure response and corresponding HTTP status
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Response<Void>> handleAuthException(AuthException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("[AuthException] code: {}, message: {}", errorCode.getCode(), errorCode.getMessage());

        Response<Void> response = Response.fail(errorCode);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}
