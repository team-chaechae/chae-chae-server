package com.project.userservice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqExternalUserUpdateDTO {

    @Valid
    @NotNull(message = "수정할 정보를 입력해주세요")
    private ExternalUser externalUser;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUser {

        @Schema(example = "홍길동")
        private String name;

        @Schema(example = "010-1234-5678")
        private String phone;

        @Schema(example = "서울시 강남구 테헤란로 123")
        private String address;
    }
}
