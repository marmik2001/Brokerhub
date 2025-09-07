package com.marmik.brokerhub.account.repository;

import com.marmik.brokerhub.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
