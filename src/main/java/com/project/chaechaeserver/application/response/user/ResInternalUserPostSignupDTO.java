package com.project.chaechaeserver.application.response.user;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResInternalUserPostSignupDTO {

    private InternalUser internalUser;

    public static ResInternalUserPostSignupDTO of(InternalUserEntity internalUserEntity) {
        return ResInternalUserPostSignupDTO.builder()
                .internalUser(InternalUser.from(internalUserEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalUser {

        private Long id;
        private String email;
        private String realName;
        private String position;
        private String role;

        public static InternalUser from(InternalUserEntity internalUserEntity) {
            return InternalUser.builder()
                    .id(internalUserEntity.getId())
                    .email(internalUserEntity.getEmail())
                    .realName(internalUserEntity.getRealName())
                    .position(internalUserEntity.getPosition().name())
                    .role(internalUserEntity.getRole().name())
                    .build();
        }
    }
}
