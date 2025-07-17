package com.mumuk.domain.allergy.service;

import com.mumuk.domain.allergy.dto.response.AllergyResponse;
import com.mumuk.domain.allergy.entity.Allergy;
import com.mumuk.domain.allergy.entity.AllergyType;
import com.mumuk.domain.allergy.repository.AllergyRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        List<AllergyResponse.ToggleResultRes.ToggleResult> ToggleResultList=new ArrayList<>();

        // 입력받은 여러 알러지를 한 번에 처리
        for (AllergyType allergyType:allergyTypeList) {

            // 알러지가 존재하는지 판단
            Optional<Allergy> existAllergy=allergyRepository.findByUserAndAllergyType(user,allergyType);

            if (existAllergy.isPresent()) {
                // 알러지가 존재한다면, 제거
                allergyRepository.delete(existAllergy.get());
                ToggleResultList.add(new AllergyResponse.ToggleResultRes.ToggleResult(allergyType,"DELETE"));
            } else{
                // "알러지 없음" 이 선택되어 있다면, 이를 먼저 삭제
                Optional<Allergy> noneAllergy=allergyRepository.findByUserAndAllergyType(user, AllergyType.NONE);
                if (noneAllergy.isPresent()){
                    allergyRepository.deleteByUserAndAllergyType(user,AllergyType.NONE);
                }

                //알러지가 존재하지 않는다면 해당 알러지 추가
                Allergy newAllergy=new Allergy(allergyType,user);
                allergyRepository.save(newAllergy);
                ToggleResultList.add(new AllergyResponse.ToggleResultRes.ToggleResult(allergyType,"ADD"));
            }
        }

        return new AllergyResponse.ToggleResultRes(ToggleResultList);
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
