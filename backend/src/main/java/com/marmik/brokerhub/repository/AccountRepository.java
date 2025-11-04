package com.marmik.brokerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marmik.brokerhub.model.Account;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
