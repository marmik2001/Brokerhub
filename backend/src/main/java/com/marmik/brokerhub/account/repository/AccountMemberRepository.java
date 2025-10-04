package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {

    List<AccountMember> findByAccountId(UUID accountId);

    List<AccountMember> findByUser(User user);

    List<AccountMember> findByUserId(UUID userId);

    Optional<AccountMember> findByUserIdAndAccountId(UUID userId, UUID accountId);

    @Query("SELECT am FROM AccountMember am WHERE am.accountId = :accountId AND am.user.id = :userId")
    Optional<AccountMember> findMembership(UUID accountId, UUID userId);
}
