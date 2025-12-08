package com.project.userservice.application.response;

import com.project.userservice.domain.model.ExternalUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResExternalUserDTO {

    private ExternalUser externalUser;

    public static ResExternalUserDTO from(ExternalUserEntity entity) {
        return ResExternalUserDTO.builder()
                .externalUser(ExternalUser.from(entity))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUser {

        @Schema(example = "1")
        private Long id;

        @Schema(example = "customer@example.com")
        private String email;

        @Schema(example = "홍길동")
        private String name;

        @Schema(example = "010-1234-5678")
        private String phone;

        @Schema(example = "서울시 강남구 테헤란로 123")
        private String address;

        private LocalDateTime createdAt;

        public static ExternalUser from(ExternalUserEntity entity) {
            return ExternalUser.builder()
                    .id(entity.getId())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .phone(entity.getPhone())
                    .address(entity.getAddress())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}
