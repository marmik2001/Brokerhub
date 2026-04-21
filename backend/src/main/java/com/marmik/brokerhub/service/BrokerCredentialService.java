package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.BrokerCredentialRepository;
import com.marmik.brokerhub.security.EnvelopeEncryptionService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for securely managing broker credentials (e.g., tokens, API keys).
 * Uses envelope encryption to protect sensitive broker data at rest.
 */
@Service
@RequiredArgsConstructor
public class BrokerCredentialService {

    private final BrokerCredentialRepository repo;
    private final AccountMemberRepository accountMemberRepo;
    private final EnvelopeEncryptionService envelope;

    private static final String LOCAL_MASTER_KEY_ID = "local-master-v1";

    /**
     * Store a token for the given accountMemberId.
     */
    @Transactional
    public BrokerCredential storeCredential(
            UUID callerUserId,
            UUID accountMemberId,
            String broker,
            String nickname,
            byte[] tokenPlain) throws Exception {

        AccountMember targetMembership = getAccountMemberOrThrow(accountMemberId);
        requireOwnerOrAdmin(callerUserId, targetMembership.getAccountId(), targetMembership.getId());

        byte[] dek = envelope.generateDek();
        try {
            EnvelopeEncryptionService.EncryptionResult enc = envelope.encryptWithDek(dek, tokenPlain);

            byte[] wrappedDek = envelope.wrapDek(dek);

            BrokerCredential bc = new BrokerCredential();
            bc.setAccountMemberId(targetMembership.getId());
            bc.setBroker(broker);
            bc.setNickname(nickname);
            bc.setTokenCipher(enc.getCipherText());
            bc.setTokenIv(enc.getIv());
            bc.setTokenEncryptedDek(wrappedDek);
            bc.setTokenKeyId(LOCAL_MASTER_KEY_ID);

            return repo.save(bc);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    /**
     * List credentials for an account member.
     */
    @Transactional(readOnly = true)
    public List<BrokerCredential> listByAccountMember(
            UUID callerUserId,
            UUID accountMemberId) {

        AccountMember targetMembership = getAccountMemberOrThrow(accountMemberId);
        requireOwnerOrAdmin(callerUserId, targetMembership.getAccountId(), targetMembership.getId());

        return repo.findByAccountMemberId(accountMemberId);
    }

    /**
     * Delete credential after access check.
     */
    @Transactional
    public void deleteCredential(UUID callerUserId, UUID credentialId) {

        BrokerCredential cred = repo.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        AccountMember targetMembership = getAccountMemberOrThrow(cred.getAccountMemberId());
        requireOwnerOrAdmin(callerUserId, targetMembership.getAccountId(), targetMembership.getId());

        repo.delete(cred);
    }

    /**
     * Decrypt token for immediate use.
     * Caller must zero returned byte[].
     */
    @Transactional(readOnly = true)
    public byte[] decryptCredentialToken(
            UUID callerUserId,
            UUID credentialId) throws Exception {

        BrokerCredential cred = repo.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        AccountMember targetMembership = getAccountMemberOrThrow(cred.getAccountMemberId());
        requireOwnerOrAdmin(callerUserId, targetMembership.getAccountId(), targetMembership.getId());

        byte[] dek = envelope.unwrapDek(cred.getTokenEncryptedDek());
        try {
            return envelope.decryptWithDek(
                    dek,
                    cred.getTokenIv(),
                    cred.getTokenCipher());
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    private AccountMember getAccountMemberOrThrow(UUID accountMemberId) {
        return accountMemberRepo.findById(accountMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Account member not found"));
    }

    private void requireOwnerOrAdmin(UUID callerUserId, UUID accountId, UUID targetAccountMemberId) {
        AccountMember callerMembership = accountMemberRepo
                .findByUserIdAndAccountId(callerUserId, accountId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this account"));

        boolean isOwner = callerMembership.getId().equals(targetAccountMemberId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(callerMembership.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only the credential owner or an account admin can perform this action");
        }
    }
}
