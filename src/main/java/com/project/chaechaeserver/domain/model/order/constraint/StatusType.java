package com.project.chaechaeserver.domain.model.order.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
