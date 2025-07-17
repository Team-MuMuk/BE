package com.mumuk.domain.allergy.controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mumuk.domain.allergy.dto.request.AllergyRequest;
import com.mumuk.domain.allergy.dto.response.AllergyResponse;
import com.mumuk.domain.allergy.entity.AllergyType;
import com.mumuk.domain.allergy.service.AllergyService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.DELETE;

import static com.mumuk.global.apiPayload.response.Response.ok;

@RestController
@RequestMapping("/api/allergy")
public class AllergyController {

    private final AllergyService allergyService;

    public AllergyController(AllergyService allergyService) {
        this.allergyService = allergyService;
    }

    @PatchMapping("/{userId}/toggle")
    public Response<AllergyResponse.ToggleResultRes> toggleAllergy(@AuthUser Long userId, @RequestBody @Valid AllergyRequest.ToggleAllergyReq request) {
        AllergyResponse.ToggleResultRes result= allergyService.toggleAllergy(userId, request.getAllergyTypeList());
        return Response.ok(ResultCode.ALLERGY_PATCH_OK,result);

    }

    @GetMapping("/{userId}")
    public Response<AllergyResponse.AllergyListRes> getAllergy(@AuthUser Long userId) {
        AllergyResponse.AllergyListRes allergyList=allergyService.getAllergyList(userId);
        return Response.ok(ResultCode.ALLERGY_GET_OK, allergyList);
    }

    @Operation(summary = "알러지 없음 선택시 다른 모든 알러지 정보 초기화")
    @DeleteMapping("/{userId}")
    public Response<String> clearAllergy(@AuthUser Long userId) {
        allergyService.clearAllAllergy(userId);
        return Response.ok(ResultCode.ALLERGY_DELETE_OK, "알러지 초기화 완료");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<String> invalidAllergyInputError(HttpMessageNotReadableException e){

        if (e.getCause() instanceof InvalidFormatException invalidFormatException) {
            if (invalidFormatException.getTargetType() !=null&&invalidFormatException.getTargetType().equals(AllergyType.class)) {
                return Response.fail(ErrorCode.ALLERGY_NOT_FOUND);
            }
        }
        return Response.fail(ErrorCode.INVALID_INPUT);
    }


}
