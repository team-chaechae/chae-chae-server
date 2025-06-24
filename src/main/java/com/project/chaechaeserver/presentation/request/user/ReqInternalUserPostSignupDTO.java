package com.project.chaechaeserver.presentation.request.user;

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
        private String email;

        @NotBlank
        private String password;

        @NotBlank
        private String realName;

        @NotBlank
        private String position;

        @NotBlank
        private String role;
    }
}