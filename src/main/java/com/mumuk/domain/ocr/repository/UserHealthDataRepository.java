package com.mumuk.domain.ocr.repository;

import com.mumuk.domain.ocr.entity.UserHealthData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHealthDataRepository extends JpaRepository<UserHealthData, Long> {
}
