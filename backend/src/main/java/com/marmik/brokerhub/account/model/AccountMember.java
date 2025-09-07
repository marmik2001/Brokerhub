package com.marmik.brokerhub.account.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "account_member")
public class AccountMember {

    @Id
    private UUID id;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // "ADMIN" or "MEMBER"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = true)
    private String rules = "{}";

    public AccountMember() {
        this.id = UUID.randomUUID();
    }

}
