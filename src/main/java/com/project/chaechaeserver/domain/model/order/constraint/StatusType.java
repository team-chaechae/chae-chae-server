package com.project.chaechaeserver.domain.model.order.constraint;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@AllArgsConstructor
public enum StatusType {

  APPROVED(Status.APPROVED, "승인"),
  COMPLETED(Status.COMPLETED, "완료"),
  CANCELLED(Status.CANCELLED, "취소");

  private final String status;
  private final String displayName;

  public static class Status {

    public static final String APPROVED = "STATUS_APPROVED";
    public static final String COMPLETED = "STATUS_COMPLETED";
    public static final String CANCELLED = "STATUS_CANCELLED";
  }

  public static final StatusType DEFAULT = APPROVED;

  public static StatusType from(String input) {
    if (!StringUtils.hasText(input)) return null;

    return Arrays.stream(values())
        .filter(e -> e.status.equalsIgnoreCase(input) || e.name().equalsIgnoreCase(input))
        .findFirst()
        .orElse(null);
  }
}
