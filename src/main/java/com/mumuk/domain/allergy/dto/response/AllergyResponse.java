package com.mumuk.domain.allergy.dto.response;

import com.mumuk.domain.allergy.entity.Allergy;
import com.mumuk.domain.allergy.entity.AllergyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

public class AllergyResponse {

    @Getter
    @AllArgsConstructor
    public static class AllergyListRes{
        private List<AllergyOption> allergyOptions;
        @Getter
        @AllArgsConstructor
        public static class AllergyOption{

            private Long id;
            private AllergyType allergyType;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ToggleResultRes{
        private List<ToggleResult> results;

        @Getter
        @AllArgsConstructor
        public static class ToggleResult{
            private AllergyType allergyType;
            private String action;
        }
    }




}
