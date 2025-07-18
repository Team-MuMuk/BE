package com.mumuk.domain.allergy.repository;

import com.mumuk.domain.allergy.entity.Allergy;
import com.mumuk.domain.allergy.entity.AllergyType;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    List<Allergy> findByUser(User user);
    List<Allergy> findByUserId(Long userId);

    Optional<Allergy> findByUserAndAllergyType(User user, AllergyType allergyType);
    boolean existsByUserAndAllergyType(User user, AllergyType allergyType);
    void deleteByUserAndAllergyType(User user, AllergyType allergyType);

    void deleteByUser(User user);

}
