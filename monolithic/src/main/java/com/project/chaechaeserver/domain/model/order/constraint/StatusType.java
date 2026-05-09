package com.project.chaechaeserver.domain.model.order.constraint;

import java.util.Arrays;
import java.util.Set;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum StatusType {

    PENDING(Status.PENDING, "주문 대기"),
    APPROVED(Status.APPROVED, "승인"),
    COMPLETED(Status.COMPLETED, "주문 완료"),
    CANCELLED(Status.CANCELLED, "취소");

    private final String status;
    private final String displayName;
    private Set<StatusType> nextAvailableStatuses;

    StatusType(String status, String displayName) {
        this.status = status;
        this.displayName = displayName;
    }

    // 상태 변경 가능 목록
    static {
        PENDING.nextAvailableStatuses = Set.of(APPROVED, CANCELLED);
        APPROVED.nextAvailableStatuses = Set.of(COMPLETED, CANCELLED);
        COMPLETED.nextAvailableStatuses = Set.of();
        CANCELLED.nextAvailableStatuses = Set.of();
    }

    public boolean canTransitionTo(StatusType targetStatus) {
        return nextAvailableStatuses.contains(targetStatus);
    }

    public static StatusType from(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }

        return Arrays.stream(values())
                .filter(e -> e.status.equalsIgnoreCase(input) || e.name().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    public static final StatusType DEFAULT = PENDING;

    public static class Status {
        public static final String PENDING = "STATUS_PENDING";
        public static final String APPROVED = "STATUS_APPROVED";
        public static final String COMPLETED = "STATUS_COMPLETED";
        public static final String CANCELLED = "STATUS_CANCELLED";

    }
}
