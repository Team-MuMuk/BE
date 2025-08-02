package com.mumuk.domain.user.repository;

import com.mumuk.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mumuk.domain.user.entity.UserRecipe;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByNameAndPhoneNumber(String name, String phoneNumber);
    Optional<User> findByLoginIdAndNameAndPhoneNumber(String loginId, String name, String phoneNumber);
    Optional<User> findByEmail(String email);
    List<User> findByFcmAgreed(Boolean fcmAgreed);




}
