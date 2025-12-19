package com.marmik.brokerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marmik.brokerhub.model.AccountMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {

    List<AccountMember> findByAccountId(UUID accountId);

    List<AccountMember> findByUserId(UUID userId);

    Optional<AccountMember> findByUserIdAndAccountId(UUID userId, UUID accountId);

    // Fetch membership by its id and account id (safety check)
    Optional<AccountMember> findByIdAndAccountId(UUID id, UUID accountId);
}
