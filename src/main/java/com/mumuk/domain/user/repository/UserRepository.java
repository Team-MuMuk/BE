package com.mumuk.domain.user.repository;

import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
 * Retrieves a user by their email address.
 *
 * @param email the email address to search for
 * @return an Optional containing the user if found, or empty if no user exists with the given email
 */
Optional<User> findByEmail(String email);
    /**
 * Checks whether a user with the specified email exists in the database.
 *
 * @param email the email address to search for
 * @return true if a user with the given email exists, false otherwise
 */
boolean existsByEmail(String email);
}
