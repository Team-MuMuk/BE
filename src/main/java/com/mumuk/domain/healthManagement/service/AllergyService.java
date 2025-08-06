package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.AllergyRequest;
import com.mumuk.domain.healthManagement.dto.response.AllergyResponse;
import com.mumuk.domain.healthManagement.entity.Allergy;
import com.mumuk.domain.healthManagement.entity.AllergyType;

import java.util.List;

public interface AllergyService {

    AllergyResponse.AllergyListRes getAllergyList(Long userId);
    AllergyResponse.AllergyListRes setAllergyList(Long userId, AllergyRequest.SetAllergyReq request);

}
