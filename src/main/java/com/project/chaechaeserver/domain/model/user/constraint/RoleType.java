package com.project.chaechaeserver.domain.model.user.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleType {

    EMPLOYEE(Role.EMPLOYEE, "직원"),
    CUSTOMER(Role.CUSTOMER, "고객"),
    ADMIN(Role.ADMIN, "관리자");

    private final String role;
    private final String displayName;

    public static class Role {
        public static final String EMPLOYEE = "ROLE_EMPLOYEE";
        public static final String CUSTOMER = "ROLE_CUSTOMER";
        public static final String ADMIN = "ROLE_ADMIN";
    }
}