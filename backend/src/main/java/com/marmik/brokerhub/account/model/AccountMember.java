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
    private UUID id = UUID.randomUUID();

    // Link to the Account
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    // Link to the User (global identity)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role; // "ADMIN" or "MEMBER"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = true)
    private String rules = "{}";
}
