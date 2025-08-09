package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.UserInfoRequest;
import com.mumuk.domain.healthManagement.dto.response.UserInfoResponse;
import com.mumuk.domain.healthManagement.entity.UserInfo;

public interface UserInfoService {

    UserInfoResponse.UserInfoRes setUserInfo(Long userId, UserInfoRequest.UserInfoReq request );

    UserInfoResponse.UserInfoRes getUserInfo(Long userId);


}
