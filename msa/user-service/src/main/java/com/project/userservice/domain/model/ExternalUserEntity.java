package com.project.userservice.domain.model;

import com.project.userservice.domain.model.constraint.RoleType;
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
@Table(name = "external_users")
public class ExternalUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

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
    public ExternalUserEntity(String email, String password, String name, String phone, String address, RoleType role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }

    public static ExternalUserEntity createCustomer(String email, String password, String name, String phone, String address) {
        return ExternalUserEntity.builder()
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .address(address)
                .role(RoleType.CUSTOMER)
                .build();
    }

    public void updateProfile(String name, String phone, String address) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (address != null) this.address = address;
    }
}
