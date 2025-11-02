package com.marmik.brokerhub.account.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JPA entity for account_member_brokers.
 *
 * Sensitive fields (tokenCipher, tokenIv, tokenEncryptedDek) are stored as
 * byte[] and
 * should only be decrypted in-memory and zeroed after use.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_member_brokers")
public class BrokerCredential {

    @Id
    @Column(name = "credential_id", nullable = false, updatable = false)
    @GeneratedValue
    private UUID credentialId;

    @Column(name = "account_member_id", nullable = false)
    private UUID accountMemberId;

    @Column(name = "broker", nullable = false, length = 50)
    private String broker;

    @Column(name = "nickname", nullable = false, length = 256)
    private String nickname;

    /**
     * AES-GCM ciphertext of the token (ciphertext includes authentication tag).
     * Keep in memory only briefly when decrypting.
     */
    @Column(name = "token_cipher", nullable = false, columnDefinition = "bytea")
    private byte[] tokenCipher;

    /**
     * 12-byte IV used for AES-GCM encryption of the token.
     */
    @Column(name = "token_iv", nullable = false, columnDefinition = "bytea")
    private byte[] tokenIv;

    /**
     * Encrypted Data Encryption Key (DEK) produced by wrapping the per-row DEK with
     * the master key.
     * Storage format: wrapper IV (12 bytes) || wrapped ciphertext bytes (as
     * documented in EnvelopeEncryptionService).
     */
    @Column(name = "token_encrypted_dek", nullable = false, columnDefinition = "bytea")
    private byte[] tokenEncryptedDek;

    /**
     * Identifier for the key used to wrap the DEK (e.g., key ID or version).
     * No secret material in this field.
     */
    @Column(name = "token_key_id", nullable = false, length = 256)
    private String tokenKeyId;
}
