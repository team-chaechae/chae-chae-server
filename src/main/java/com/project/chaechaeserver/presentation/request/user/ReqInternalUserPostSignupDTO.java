package com.project.chaechaeserver.presentation.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqInternalUserPostSignupDTO {

    @Valid
    @NotNull(message = "회원정보를 입력해주세요")
    private InternalUser internalUser;

    @Getter
    public static class InternalUser {

        @Email
        @Schema(example = "john.doe@example.com")
        private String email;

        @NotBlank
        @Schema(example = "securePassword123!")
        private String password;

        @NotBlank
        @Schema(example = "홍길동")
        private String realName;

        @NotBlank
        @Schema(
                example = "POSITION_STAFF",
                allowableValues = {
                        "POSITION_STAFF", "POSITION_ASSISTANT_MANAGER", "POSITION_SENIOR_ASSISTANT_MANAGER",
                        "POSITION_MANAGER", "POSITION_DEPUTY_GENERAL_MANAGER", "POSITION_GENERAL_MANAGER"
                }
        )
        private String position;

        @NotBlank
        @Schema(example = "EMPLOYEE")
        private String role;
    }
}