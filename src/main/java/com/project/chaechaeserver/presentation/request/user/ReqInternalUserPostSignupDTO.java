package com.project.chaechaeserver.presentation.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @NotBlank(message = "이메일을 입력해주세요.")
        @Schema(example = "john.doe@example.com")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,16}$",
                message = "비밀번호는 8~16자리수여야 합니다. 영문 대소문자, 숫자, 특수문자를 1개 이상 포함해야 합니다.")
        @Schema(example = "Password123!")
        private String password;

        @NotBlank(message = "이름을 입력해주세요.")
        @Schema(example = "홍길동")
        private String realName;

        @NotBlank(message = "직급을 입력해주세요.")
        @Schema(
                example = "POSITION_STAFF",
                allowableValues = {
                        "POSITION_STAFF", "POSITION_ASSISTANT_MANAGER", "POSITION_SENIOR_ASSISTANT_MANAGER",
                        "POSITION_MANAGER", "POSITION_DEPUTY_GENERAL_MANAGER", "POSITION_GENERAL_MANAGER"
                }
        )
        private String position;
    }
}