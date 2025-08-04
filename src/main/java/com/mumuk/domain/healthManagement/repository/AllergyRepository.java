package com.mumuk.domain.healthManagement.repository;

import com.mumuk.domain.healthManagement.entity.Allergy;
import com.mumuk.domain.healthManagement.entity.AllergyType;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    List<Allergy> findByUser(User user);


    void deleteByUser(User user);
}
