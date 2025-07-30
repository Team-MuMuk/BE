package com.mumuk.domain.allergy.service;

import com.mumuk.domain.allergy.dto.response.AllergyResponse;
import com.mumuk.domain.allergy.entity.Action;
import com.mumuk.domain.allergy.entity.Allergy;
import com.mumuk.domain.allergy.entity.AllergyType;
import com.mumuk.domain.allergy.repository.AllergyRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AllergyServiceImpl implements AllergyService {

    private final AllergyRepository allergyRepository;
    private final UserRepository userRepository;

    public AllergyServiceImpl(AllergyRepository allergyRepository, UserRepository userRepository) {
        this.allergyRepository = allergyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AllergyResponse.AllergyListRes getAllergyList(Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new AuthException(ErrorCode.USER_NOT_FOUND));

        List<Allergy> allergyList = allergyRepository.findByUser(user);

        // 알러지 목록과 선택 여부까지 불러오도록
        List<AllergyResponse.AllergyListRes.AllergyOption> allergyListRes = allergyList.stream()
                .map(allergy -> new AllergyResponse.AllergyListRes.AllergyOption(
                        allergy.getId(),
                        allergy.getAllergyType()))

                .collect(Collectors.toList());

        return new AllergyResponse.AllergyListRes(allergyListRes);
    }

    @Override
    @Transactional
    public AllergyResponse.ToggleResultRes toggleAllergy(Long userId, List<AllergyType> allergyTypeList) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new AuthException(ErrorCode.USER_NOT_FOUND));

        // NONE 과 다른 알러지 타입은 동시에 입력 불가
        if (allergyTypeList.contains(AllergyType.NONE) && allergyTypeList.size()>1){
            throw new GlobalException(ErrorCode.ALLERGY_NONE_WITH_OTHERS);
        }

        // 사용자의 기존 알러지 정보 삭제
        allergyRepository.deleteByUser(user);

        // 입력받은 알러지 타입 정보 목록에 대해서, 하나하나 알러지 객체 목록으로 변환
        List<Allergy> allergyList = allergyTypeList.stream()
                .map(allergyType -> new Allergy(allergyType,user))
                .toList();

        // 알러지 객체 목록 데이터베이스에 저장
        allergyRepository.saveAll(allergyList);

        // 입력한 알러지 객체 목록 반환
        List<AllergyResponse.ToggleResultRes.ToggleResult> toggleResultList = allergyTypeList.stream()
                .map(allergyType -> new AllergyResponse.ToggleResultRes.ToggleResult(allergyType,Action.ADDED))
                .toList();


        return new AllergyResponse.ToggleResultRes(toggleResultList);
    }

    @Override
    @Transactional
    public void clearAllAllergy(Long userId) {
        // 알러지 정보 초기화, None 선택시 다른 선택된 알러지 초기화 용도
        User user=userRepository.findById(userId)
                .orElseThrow(()->new AuthException(ErrorCode.USER_NOT_FOUND));
        allergyRepository.deleteByUser(user);
    }

}
