package com.mumuk.domain.allergy.dto.request;

import com.mumuk.domain.allergy.entity.Allergy;
import com.mumuk.domain.allergy.entity.AllergyType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class AllergyRequest {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ToggleAllergyReq {
        private List<AllergyType> allergyTypeList;
    }
}
