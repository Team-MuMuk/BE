package com.mumuk.domain.user.service;



import com.mumuk.domain.user.converter.MypageConverter;
import com.mumuk.domain.user.dto.request.MypageRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class MypageServiceImpl implements MypageService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;


    public MypageServiceImpl(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    @Override
    @Transactional
    public UserResponse.ProfileInfoDTO profileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return MypageConverter.toProfileInfoDTO(user);
    }

    @Override
    @Transactional
    public void editProfile(MypageRequest.EditProfileReq request,String accessToken) {
        String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(accessToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        user.setName(request.getName());
        user.setNickName(request.getNickName());
        user.setProfileImage(request.getProfileImage());
        user.setStatusMessage(request.getStatusMessage());
        userRepository.save(user);

    }




}
