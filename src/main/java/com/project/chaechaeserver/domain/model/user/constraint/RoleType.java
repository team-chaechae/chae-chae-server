package com.project.chaechaeserver.domain.model.user.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleType {

    EMPLOYEE(Role.EMPLOYEE),
    CUSTOMER(Role.CUSTOMER),
    ADMIN(Role.ADMIN);

    private final String role;

    private static class Role {
        public static final String EMPLOYEE = "직원";
        public static final String CUSTOMER = "고객";
        public static final String ADMIN = "관리자";
    }
}