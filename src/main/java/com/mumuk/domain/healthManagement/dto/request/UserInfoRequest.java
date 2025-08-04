package com.mumuk.domain.healthManagement.dto.request;

import com.mumuk.domain.healthManagement.entity.Gender;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserInfoRequest {

    @Getter
    @NoArgsConstructor
    public static class UserInfoReq {

        private Gender gender;
        @Min(value = 1, message = "키는 1cm 이상이어야 합니다")
        private Long height;
        @Min(value = 1, message = "몸무게는 1kg 이상이어야 합니다")
        private Long weight;

        public void setHeight(Long height) { this.height = height; }
        public void setWeight(Long weight) { this.weight = weight; }
        public void setGender(Gender gender) { this.gender = gender; }
    }


}
