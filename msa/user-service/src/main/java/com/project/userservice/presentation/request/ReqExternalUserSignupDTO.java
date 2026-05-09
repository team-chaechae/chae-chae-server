package com.project.userservice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqExternalUserSignupDTO {

    @Valid
    @NotNull(message = "회원정보를 입력해주세요")
    private ExternalUser externalUser;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUser {

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일을 입력해주세요.")
        @Schema(example = "customer@example.com")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,16}$",
                message = "비밀번호는 8~16자리수여야 합니다. 영문 대소문자, 숫자, 특수문자를 1개 이상 포함해야 합니다.")
        @Schema(example = "Password123!")
        private String password;

        @NotBlank(message = "이름을 입력해주세요.")
        @Schema(example = "홍길동")
        private String name;

        @Schema(example = "010-1234-5678")
        private String phone;

        @Schema(example = "서울시 강남구 테헤란로 123")
        private String address;
    }
}
