package com.project.chaechaeserver.domain.model.user.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PositionType {

    STAFF(Position.STAFF, "사원"),
    ASSISTANT_MANAGER(Position.ASSISTANT_MANAGER, "주임"),
    SENIOR_ASSISTANT_MANAGER(Position.SENIOR_ASSISTANT_MANAGER, "대리"),
    MANAGER(Position.MANAGER, "과장"),
    DEPUTY_GENERAL_MANAGER(Position.DEPUTY_GENERAL_MANAGER, "차장"),
    GENERAL_MANAGER(Position.GENERAL_MANAGER, "부장"),
    DIRECTOR(Position.DIRECTOR, "이사"),
    EXECUTIVE_DIRECTOR(Position.EXECUTIVE_DIRECTOR, "상무"),
    SENIOR_EXECUTIVE_DIRECTOR(Position.SENIOR_EXECUTIVE_DIRECTOR, "전무"),
    VICE_PRESIDENT(Position.VICE_PRESIDENT, "부사장"),
    PRESIDENT(Position.PRESIDENT, "사장");

    private final String position;
    private final String displayName;

    public static class Position {
        public static final String STAFF = "POSITION_STAFF";
        public static final String ASSISTANT_MANAGER = "POSITION_ASSISTANT_MANAGER";
        public static final String SENIOR_ASSISTANT_MANAGER = "POSITION_SENIOR_ASSISTANT_MANAGER";
        public static final String MANAGER = "POSITION_MANAGER";
        public static final String DEPUTY_GENERAL_MANAGER = "POSITION_DEPUTY_GENERAL_MANAGER";
        public static final String GENERAL_MANAGER = "POSITION_GENERAL_MANAGER";
        public static final String DIRECTOR = "POSITION_DIRECTOR";
        public static final String EXECUTIVE_DIRECTOR = "POSITION_EXECUTIVE_DIRECTOR";
        public static final String SENIOR_EXECUTIVE_DIRECTOR = "POSITION_SENIOR_EXECUTIVE_DIRECTOR";
        public static final String VICE_PRESIDENT = "POSITION_VICE_PRESIDENT";
        public static final String PRESIDENT = "POSITION_PRESIDENT";
    }
}