package com.mumuk.domain.allergy.service;

import com.mumuk.domain.allergy.dto.response.AllergyResponse;
import com.mumuk.domain.allergy.entity.AllergyType;

import java.util.List;

public interface AllergyService {

    AllergyResponse.AllergyListRes getAllergyList(Long userId);
    AllergyResponse.ToggleResultRes toggleAllergy(Long userId, List<AllergyType> allergyTypeList);
    void clearAllAllergy(Long userId);
}
