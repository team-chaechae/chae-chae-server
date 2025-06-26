package com.project.chaechaeserver.infrastructure.security;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Builder
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private User user;

    public static CustomUserDetails of(InternalUserEntity internalUserEntity) {
        return CustomUserDetails.builder()
                .user(User.from(internalUserEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {

        private Long id;
        private String email;
        private String password;
        private RoleType role;

        public static User from(InternalUserEntity internalUserEntity) {
            return User.builder()
                    .id(internalUserEntity.getId())
                    .email(internalUserEntity.getEmail())
                    .password(internalUserEntity.getPassword())
                    .role(internalUserEntity.getRole())
                    .build();
        }
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        RoleType role = user.getRole();
        String authority = role.getRole();

        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(authority);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(simpleGrantedAuthority);

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}