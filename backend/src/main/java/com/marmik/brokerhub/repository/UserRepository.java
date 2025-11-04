package com.marmik.brokerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.marmik.brokerhub.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    @Query("""
                SELECT u FROM User u
                WHERE LOWER(u.email) = LOWER(:identifier)
                   OR u.loginId = :identifier
            """)
    Optional<User> findByLoginIdOrEmailIgnoreCase(String identifier);
}
