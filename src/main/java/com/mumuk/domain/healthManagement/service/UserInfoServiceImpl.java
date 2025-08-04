package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.UserInfoRequest;
import com.mumuk.domain.healthManagement.dto.response.UserInfoResponse;
import com.mumuk.domain.healthManagement.entity.UserInfo;
import com.mumuk.domain.healthManagement.repository.UserInfoRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    public UserInfoServiceImpl (UserRepository userRepository, UserInfoRepository userInfoRepository){
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
    }
    @Override
    @Transactional
    public UserInfoResponse.UserInfoRes setUserInfo(Long userId, UserInfoRequest.UserInfoReq request) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //userInfo가 존재하는지 확인하고 수정하기
        UserInfo userInfo =userInfoRepository.findByUserId(userId)
                        .orElseGet(()->{
                            UserInfo newUserinfo =new UserInfo();
                            newUserinfo.setUser(user);
                            return newUserinfo;
                        });

        userInfo.setGender(request.getGender());
        userInfo.setHeight(request.getHeight());
        userInfo.setWeight(request.getWeight());

        userInfoRepository.save(userInfo);

        UserInfoResponse.UserInfoRes userInfoRes =new UserInfoResponse.UserInfoRes(userInfo.getGender(),userInfo.getHeight(),userInfo.getWeight());
        return userInfoRes;
    }

    @Override
    @Transactional
    public UserInfoResponse.UserInfoRes getUserInfo(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserInfo userInfo=userInfoRepository.findByUserId(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USERINFO_NOT_FOUND));

        return new UserInfoResponse.UserInfoRes(userInfo.getGender(), userInfo.getHeight(), userInfo.getWeight());
    }
}
