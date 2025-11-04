package com.marmik.brokerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marmik.brokerhub.model.BrokerCredential;

import java.util.List;
import java.util.UUID;

public interface BrokerCredentialRepository extends JpaRepository<BrokerCredential, UUID> {

    /**
     * Find all broker credentials for a given account_member (account_member.id).
     */
    List<BrokerCredential> findByAccountMemberId(UUID accountMemberId);
}
