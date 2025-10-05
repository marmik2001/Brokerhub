package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.model.User;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AccountMemberRepository memberRepo;

    public UserService(UserRepository userRepo, AccountMemberRepository memberRepo) {
        this.userRepo = userRepo;
        this.memberRepo = memberRepo;
    }

    /**
     * Returns user profile + all memberships.
     */
    public Optional<Map<String, Object>> getUserProfile(UUID userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty())
            return Optional.empty();

        User user = userOpt.get();
        List<AccountMember> memberships = memberRepo.findByUserId(userId);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("loginId", user.getLoginId());
        profile.put("email", user.getEmail());
        profile.put("name", user.getMemberName());
        profile.put("accounts", memberships.stream().map(m -> Map.of(
                "accountId", m.getAccountId(),
                "role", m.getRole())).toList());

        return Optional.of(profile);
    }
}
