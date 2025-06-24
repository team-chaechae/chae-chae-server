package com.project.chaechaeserver.domain.model.user.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PositionType {

    STAFF(Position.STAFF),
    ASSISTANT_MANAGER(Position.ASSISTANT_MANAGER),
    SENIOR_ASSISTANT_MANAGER(Position.SENIOR_ASSISTANT_MANAGER),
    MANAGER(Position.MANAGER),
    DEPUTY_GENERAL_MANAGER(Position.DEPUTY_GENERAL_MANAGER),
    GENERAL_MANAGER(Position.GENERAL_MANAGER),
    DIRECTOR(Position.DIRECTOR),
    EXECUTIVE_DIRECTOR(Position.EXECUTIVE_DIRECTOR),
    SENIOR_EXECUTIVE_DIRECTOR(Position.SENIOR_EXECUTIVE_DIRECTOR),
    VICE_PRESIDENT(Position.VICE_PRESIDENT),
    PRESIDENT(Position.PRESIDENT);

    private final String position;

    private static class Position {
        public static final String STAFF = "사원";
        public static final String ASSISTANT_MANAGER = "주임";
        public static final String SENIOR_ASSISTANT_MANAGER = "대리";
        public static final String MANAGER = "과장";
        public static final String DEPUTY_GENERAL_MANAGER = "차장";
        public static final String GENERAL_MANAGER = "부장";
        public static final String DIRECTOR = "이사";
        public static final String EXECUTIVE_DIRECTOR = "상무";
        public static final String SENIOR_EXECUTIVE_DIRECTOR = "전무";
        public static final String VICE_PRESIDENT = "부사장";
        public static final String PRESIDENT = "사장";
    }
}
