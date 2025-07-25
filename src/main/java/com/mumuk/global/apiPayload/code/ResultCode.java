package com.mumuk.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResultCode implements BaseCode {

    OK(HttpStatus.OK, "COMMON_200", "성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "COMMON_201", "성공적으로 생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON_204", "성공적으로 삭제되었습니다."),

    // Recipe Success
    RECIPE_CREATE_OK(HttpStatus.CREATED, "RECIPE_201", "레시피 등록 성공"),
    RECIPE_DELETE_OK(HttpStatus.NO_CONTENT, "RECIPE_204", "레시피 삭제 성공"),
    RECIPE_FETCH_OK(HttpStatus.OK, "RECIPE_200", "레시피 조회 성공"),

    // User Success
    USER_FETCH_OK(HttpStatus.OK, "USER_200", "유저 정보 조회 성공"),
    TOKEN_REISSUE_OK(HttpStatus.OK, "TOKEN_200", "토큰 재발급 성공"),
    PW_REISSUE_OK(HttpStatus.OK, "USER_200", "유저 비밀번호 변경 성공"),
    USER_LOGOUT_OK(HttpStatus.OK, "USER_200", "유저 로그아웃 성공"),
    USER_WITHDRAW_OK(HttpStatus.NO_CONTENT, "USER_204", "유저 탈퇴 성공"),
    USER_LOGIN_OK(HttpStatus.OK, "USER_200", "유저 로그인 성공"),
    USER_SIGNUP_OK(HttpStatus.CREATED, "USER_201", "유저 회원가입 성공"),

    SEND_ID_BY_SMS_OK(HttpStatus.NO_CONTENT, "USER_204", "아이디 변경을 위한 SMS 인증 발송 성공"),
    SEND_PW_BY_SMS_OK(HttpStatus.NO_CONTENT, "USER_204", "비밀번호 변경을 위한 SMS 인증 발송 성공"),
    EDIT_PROFILE_OK(HttpStatus.OK, "USER_200", "프로필 수정 성공"),

    // UserRecipe Success
    USER_RECIPE_OK(HttpStatus.OK, "USER_RECIPE_200", "유저의 해당 레시피 조회 정보 저장 성공"),
    RECENT_RECIPE_OK(HttpStatus.OK, "USER_RECIPE_201", "최근 조회한 레시피 불러오기 성공"),

    //Ingredient Success
    INGREDIENT_REGISTER_OK(HttpStatus.OK, "INGREDIENT_200","재료등록 성공"),
    INGREDIENT_RETRIEVE_OK(HttpStatus.OK, "INGREDIENT_200","등록하신 재료조회 성공"),
    INGREDIENT_UPDATE_OK(HttpStatus.OK, "INGREDIENT_200","재료 수정 성공"),
    INGREDIENT_DELETE_OK(HttpStatus.OK, "INGREDIENT_200","재료 삭제 성공"),
    CLOSED_DATE_INGREDIENT_RETRIEVE_OK(HttpStatus.OK, "INGREDIENT_200","유통기한 임박 재료 조회 성공"),


    // Search Success
    SEARCH_RECIPE_OK(HttpStatus.OK, "SEARCH_200", "검색결과목록 조회 성공"),
    SEARCH_DETAILRECIPE_OK(HttpStatus.OK, "SEARCH_200", "세부검색결과 조회 성공"),
    SEARCH_AUTOCOMPLETE_OK(HttpStatus.OK, "SEARCH_200","자동완성 성공"),
    SEARCH_SAVE_RECENTSEARCHES_OK(HttpStatus.CREATED,"SEARCH_201", "최근 검색어 저장 성공"),
    SEARCH_DELETE_RECENTSEARCHES_OK(HttpStatus.NO_CONTENT,"SEARCH_204", "최근 검색어 삭제 성공"),
    SEARCH_GET_RECENTSEARCHES_OK(HttpStatus.OK,"SEARCH_200", "최근 검색어 조회 성공"),
    SEARCH_GET_TRENDKEYWORDS_OK(HttpStatus.OK, "SEARCH_400", "인기 검색어 조회 성공"),
    SEARCH_GET_RECOMMENDED_RECIPE_OK(HttpStatus.OK,"SEARCH400","추천 검색어 조회 성공"),

    //Allergy Success
    ALLERGY_PATCH_OK(HttpStatus.OK, "ALLERGY_200","알러지 정보 변경 성공"),
    ALLERGY_DELETE_OK(HttpStatus.NO_CONTENT, "ALLERGY_204","알러지 정보 삭제 성공"),
    ALLERGY_GET_OK(HttpStatus.OK, "ALLERGY_200","알러지 정보 조회 성공");



    private final HttpStatus status;
    private final String code;
    private final String message;

    // 인터페이스 구현
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
