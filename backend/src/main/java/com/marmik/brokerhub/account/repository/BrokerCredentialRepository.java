package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.BrokerCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrokerCredentialRepository extends JpaRepository<BrokerCredential, UUID> {

    /**
     * Find all broker credentials for a given account_member (account_member.id).
     */
    List<BrokerCredential> findByAccountMemberId(UUID accountMemberId);
}
