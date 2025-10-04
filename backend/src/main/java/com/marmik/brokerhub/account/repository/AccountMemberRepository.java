package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.AccountMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {
    Optional<AccountMember> findByLoginId(String loginId);

    @Query("SELECT m FROM AccountMember m WHERE m.loginId = :identifier OR LOWER(m.email) = LOWER(:identifier)")
    Optional<AccountMember> findByLoginIdOrEmailIgnoreCase(@Param("identifier") String identifier);
}
