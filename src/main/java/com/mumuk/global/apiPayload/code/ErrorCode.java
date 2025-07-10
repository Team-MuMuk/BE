package com.mumuk.global.apiPayload.code;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode {

    // Common Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러입니다. 관리자에게 문의하세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "찾을 수 없는 요청입니다."),

    // Recipe Error
    RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECIPE_404", "레시피를 찾을 수 없습니다."),
    RECIPE_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECIPE_409", "이미 존재하는 레시피입니다."),
    RECIPE_INVALID_DATA(HttpStatus.BAD_REQUEST, "RECIPE_400", "잘못된 레시피 데이터입니다."),

    // User Error
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_401", "로그인 정보가 없습니다."),
    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "USER_401", "로그인 하지 않았습니다."),
    USER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "USER_403", "권한이 없습니다."),

    JWT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT_500", "JWT 생성에 실패했습니다."),
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401", "유효하지 않은 JWT 토큰입니다."),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_EX", "만료된 JWT 토큰입니다."),

    // KaKao
    KAKAO_JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KAKAO_500_JSON", "카카오 프로필 파싱 중 오류가 발생했습니다."),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_502_API", "카카오 서버와의 통신 중 오류가 발생했습니다."),
    KAKAO_INVALID_GRANT(HttpStatus.UNAUTHORIZED, "KAKAO_401_INVALID_GRANT", "유효하지 않거나 만료된 인가 코드입니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_401_AUTH_FAILED", "카카오 인증에 실패했습니다."),
    ALREADY_REGISTERED_WITH_OTHER_LOGIN(HttpStatus.CONFLICT, "AUTH_409_ALREADY_REGISTERED", "해당 이메일은 다른 로그인 방식으로 이미 가입되어 있습니다."),

    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 사용 중인 아이디 입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 사용 중인 이메일 입니다."),
    ID_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_ID_MISMATCH", "아이디가 일치하지 않습니다."),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 가입된 휴대폰 번호 입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_PW_MISMATCH", "비밀번호가 일치하지 않습니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_LOGIN_ID", "아이디 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_PW", "비밀번호는 8-15자이며 영문, 숫자, 특수문자를 포함해야 합니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_NICKNAME", "닉네임은 10자 이내만 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
