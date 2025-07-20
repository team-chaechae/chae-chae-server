package com.project.chaechaeserver.application.response.user;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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

    public static ResInternalUserPostSignupDTO from(InternalUserEntity internalUserEntity) {
        return ResInternalUserPostSignupDTO.builder()
                .internalUser(InternalUser.from(internalUserEntity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalUser {

        @Schema(example = "1")
        private Long id;

        @Schema(example = "john.doe@example.com")
        private String email;

        @Schema(example = "홍길동")
        private String realName;

        @Schema(example = "P-20250001")
        private String employeeCode;

        @Schema(example = "POSITION_MANAGER")
        private String position;

        public static InternalUser from(InternalUserEntity internalUserEntity) {
            return InternalUser.builder()
                    .id(internalUserEntity.getId())
                    .email(internalUserEntity.getEmail())
                    .realName(internalUserEntity.getRealName())
                    .employeeCode(internalUserEntity.getEmployeeCode())
                    .position(internalUserEntity.getPosition().name())
                    .build();
        }
    }
}
