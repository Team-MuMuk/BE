package com.mumuk.domain.healthManagement.controller;

import com.mumuk.domain.healthManagement.dto.request.AllergyRequest;
import com.mumuk.domain.healthManagement.dto.request.HealthGoalRequest;
import com.mumuk.domain.healthManagement.dto.request.UserInfoRequest;
import com.mumuk.domain.healthManagement.dto.response.AllergyResponse;
import com.mumuk.domain.healthManagement.dto.response.HealthGoalResponse;
import com.mumuk.domain.healthManagement.dto.response.UserInfoResponse;
import com.mumuk.domain.healthManagement.service.AllergyService;
import com.mumuk.domain.healthManagement.service.HealthGoalService;
import com.mumuk.domain.healthManagement.service.UserInfoService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class healthManagementController {

    private final AllergyService allergyService;
    private final HealthGoalService healthGoalService;
    private final UserInfoService userInfoService;


    public healthManagementController(AllergyService allergyService, HealthGoalService healthGoalService, UserInfoService userInfoService) {
        this.allergyService = allergyService;
        this.healthGoalService = healthGoalService;
        this.userInfoService = userInfoService;
    }

    @Operation(summary = "사용자의 신체정보 변경")
    @PatchMapping("/userinfo")
    public Response<UserInfoResponse.UserInfoRes> putUserInfo(@AuthUser Long userId, @RequestBody @Valid UserInfoRequest.UserInfoReq request) {
        UserInfoResponse.UserInfoRes result=userInfoService.setUserInfo(userId, request);
        return Response.ok(ResultCode.USERINFO_PUT_OK, result);
    }

    @Operation(summary = "사용자의 신체정보 조회")
    @GetMapping("/userInfo")
    public Response<UserInfoResponse.UserInfoRes> getUserInfo(@AuthUser Long userId) {
        UserInfoResponse.UserInfoRes result=userInfoService.getUserInfo(userId);
        return Response.ok(ResultCode.USERINFO_GET_OK, result);
    }

    @Operation(summary = "사용자의 알러지 정보 변경")
    @PutMapping("/allergies")
    public Response<AllergyResponse.AllergyListRes> putAllergy(@AuthUser Long userId, @RequestBody @Valid AllergyRequest.SetAllergyReq request) {
        AllergyResponse.AllergyListRes result=allergyService.setAllergyList(userId, request);
        return Response.ok(ResultCode.ALLERGY_PUT_OK,result);
    }

    @Operation(summary = "사용자의 알러지 정보 조회")
    @GetMapping("/allergies")
    public Response<AllergyResponse.AllergyListRes> getAllergy(@AuthUser Long userId) {
        AllergyResponse.AllergyListRes result=allergyService.getAllergyList(userId);
        return Response.ok(ResultCode.ALLERGY_GET_OK,result);
    }

    @Operation(summary = "사용자의 건강목표 정보 변경")
    @PutMapping("/health-goals")
    public Response<HealthGoalResponse.HealthGoalListRes> putHealthGoal(@AuthUser Long userId, @RequestBody @Valid HealthGoalRequest.SetHealthGoalReq request) {
        HealthGoalResponse.HealthGoalListRes result=healthGoalService.setHealthGoalList(userId, request);
        return Response.ok(ResultCode.HEALTHGOAL_PUT_OK,result);
    }

    @Operation(summary = "사용자의 건강목표 정보 조회")
    @GetMapping("/health-goals")
    public Response<HealthGoalResponse.HealthGoalListRes> getHealthGoal(@AuthUser Long userId) {
        HealthGoalResponse.HealthGoalListRes result=healthGoalService.getHealthGoalList(userId);
        return Response.ok(ResultCode.HEALTHGOAL_GET_OK,result);
    }





}
