package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.AccountMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {
    Optional<AccountMember> findByLoginId(String loginId);
}
