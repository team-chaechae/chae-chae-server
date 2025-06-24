package com.project.chaechaeserver.domain.model.user;

import com.project.chaechaeserver.domain.model.user.constraint.PositionType;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "internal_users")
public class InternalUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_user_id")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "real_name", nullable = false)
    private String realName;

    @Column(name = "position", nullable = false)
    @Enumerated(EnumType.STRING)
    private PositionType position;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public InternalUserEntity (String email, String password, String realName, PositionType position, RoleType role) {
        this.email = email;
        this.password = password;
        this.realName = realName;
        this.position = position;
        this.role = role;
    }

    // -- 생성 메서드 -- //
    public static InternalUserEntity createInternalUser(String email, String password, String realName, PositionType position, RoleType role) {
        return InternalUserEntity.builder()
                .email(email)
                .password(password)
                .realName(realName)
                .position(position)
                .role(role)
                .build();
    }
}
