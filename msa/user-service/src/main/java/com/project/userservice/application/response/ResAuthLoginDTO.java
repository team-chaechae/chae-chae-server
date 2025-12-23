package com.project.userservice.application.response;

import com.project.userservice.domain.model.constraint.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResAuthLoginDTO {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String role;
    private String realName;

    public static ResAuthLoginDTO of(String accessToken, String refreshToken,
                                      String email, RoleType role, String realName) {
        return ResAuthLoginDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(email)
                .role(role.name())
                .realName(realName)
                .build();
    }
}
