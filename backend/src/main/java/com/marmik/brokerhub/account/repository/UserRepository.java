package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
