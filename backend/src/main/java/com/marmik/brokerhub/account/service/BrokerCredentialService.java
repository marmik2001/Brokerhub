package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.BrokerCredential;
import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.BrokerCredentialRepository;
import com.marmik.brokerhub.security.EnvelopeEncryptionService;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Broker credential service that stores envelope-encrypted tokens per
 * account_member.
 *
 * We validate membership using AccountAccessValidator.requireMembership(...)
 * which checks the caller
 * is a member of the same account as the referenced account_member.
 */
@Service
@RequiredArgsConstructor
public class BrokerCredentialService {

    private final BrokerCredentialRepository repo;
    private final AccountMemberRepository accountMemberRepo;
    private final EnvelopeEncryptionService envelope;
    private final AccountAccessValidator accessValidator;

    // key id for local master wrapper; in future this could be versioned
    private static final String LOCAL_MASTER_KEY_ID = "local-master-v1";

    /**
     * Store a token for the given accountMemberId.
     * - callerUserId is the authenticated user performing the operation; we
     * validate they are a member.
     */
    @Transactional
    public BrokerCredential storeCredential(UUID callerUserId, UUID accountMemberId, String broker, String nickname,
            byte[] tokenPlain)
            throws Exception {
        AccountMember am = accountMemberRepo.findById(accountMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Account member not found"));

        // allow member of the account to store credentials
        accessValidator.requireMembership(callerUserId, am.getAccountId());

        // generate DEK
        byte[] dek = envelope.generateDek();
        try {
            // encrypt token with DEK
            EnvelopeEncryptionService.EncryptionResult enc = envelope.encryptWithDek(dek, tokenPlain);

            // wrap DEK with master key
            byte[] wrappedDek = envelope.wrapDek(dek);

            BrokerCredential bc = new BrokerCredential();
            bc.setAccountMemberId(accountMemberId);
            bc.setBroker(broker);
            bc.setNickname(nickname);
            bc.setTokenCipher(enc.getCipherText());
            bc.setTokenIv(enc.getIv());
            bc.setTokenEncryptedDek(wrappedDek);
            bc.setTokenKeyId(LOCAL_MASTER_KEY_ID);

            return repo.save(bc);
        } finally {
            // zero DEK for safety
            Arrays.fill(dek, (byte) 0);
        }
    }

    /**
     * List credentials for the account member after access check.
     */
    @Transactional(readOnly = true)
    public List<BrokerCredential> listByAccountMember(UUID callerUserId, UUID accountMemberId) {
        AccountMember am = accountMemberRepo.findById(accountMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Account member not found"));

        // allow any member of the account to list
        accessValidator.requireMembership(callerUserId, am.getAccountId());

        return repo.findByAccountMemberId(accountMemberId);
    }

    /**
     * Delete credential by id after access check (ensures caller is a member of the
     * owning account).
     */
    @Transactional
    public void deleteCredential(UUID callerUserId, UUID credentialId) {
        BrokerCredential cred = repo.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        AccountMember am = accountMemberRepo.findById(cred.getAccountMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Account member not found"));

        // allow any member of the account to delete
        accessValidator.requireMembership(callerUserId, am.getAccountId());

        repo.delete(cred);
    }

    /**
     * Decrypt token for immediate use.
     * Caller must zero returned byte[] after use.
     */
    @Transactional(readOnly = true)
    public byte[] decryptCredentialToken(UUID callerUserId, UUID credentialId) throws Exception {
        BrokerCredential cred = repo.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("credential not found"));

        AccountMember am = accountMemberRepo.findById(cred.getAccountMemberId())
                .orElseThrow(() -> new IllegalArgumentException("account member not found"));

        accessValidator.requireMembership(callerUserId, am.getAccountId());

        byte[] wrapped = cred.getTokenEncryptedDek();
        byte[] dek = envelope.unwrapDek(wrapped);
        try {
            byte[] plain = envelope.decryptWithDek(dek, cred.getTokenIv(), cred.getTokenCipher());
            return plain;
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }
}
