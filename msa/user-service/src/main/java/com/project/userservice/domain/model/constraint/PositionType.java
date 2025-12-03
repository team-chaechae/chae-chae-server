package com.project.userservice.domain.model.constraint;

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
    GENERAL_MANAGER(Position.GENERAL_MANAGER, "부장");

    private final String position;
    private final String displayName;

    public static class Position {
        public static final String STAFF = "POSITION_STAFF";
        public static final String ASSISTANT_MANAGER = "POSITION_ASSISTANT_MANAGER";
        public static final String SENIOR_ASSISTANT_MANAGER = "POSITION_SENIOR_ASSISTANT_MANAGER";
        public static final String MANAGER = "POSITION_MANAGER";
        public static final String DEPUTY_GENERAL_MANAGER = "POSITION_DEPUTY_GENERAL_MANAGER";
        public static final String GENERAL_MANAGER = "POSITION_GENERAL_MANAGER";
    }

    public static PositionType from(String position) {

        if (position == null || position.isBlank()) {
            throw new IllegalArgumentException("직책 값이 비어 있습니다.");
        }

        return switch (position.toUpperCase()) {
            case Position.STAFF -> PositionType.STAFF;
            case Position.ASSISTANT_MANAGER -> PositionType.ASSISTANT_MANAGER;
            case Position.SENIOR_ASSISTANT_MANAGER -> PositionType.SENIOR_ASSISTANT_MANAGER;
            case Position.MANAGER -> PositionType.MANAGER;
            case Position.DEPUTY_GENERAL_MANAGER -> PositionType.DEPUTY_GENERAL_MANAGER;
            case Position.GENERAL_MANAGER -> PositionType.GENERAL_MANAGER;
            default -> throw new IllegalArgumentException("지원하지 않는 직책입니다: " + position);
        };
    }
}
