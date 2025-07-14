package com.project.chaechaeserver.infrastructure.config;

import com.project.chaechaeserver.application.global.constants.ResCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        // Security Scheme 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security Requirement 정의
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");

        return new OpenAPI()
                .info(new Info().title("CHAE-CHAE API")
                        .description("Chae-Chae Application API Documentation")
                        .version("v1.0"))
                .addSecurityItem(securityRequirement)
                .schemaRequirement("BearerAuth", securityScheme);
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {

            // 성공 시: 실제 응답을 감싼 schema 생성
            addSuccessWrapperSchema(operation);

            // 실패 시: 공통 에러 응답 스키마 등록
            addErrorSchema(operation);

            return operation;
        };
    }

    private void addSuccessWrapperSchema(Operation operation) {
        ApiResponses responses = operation.getResponses();

        for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
            String statusCode = entry.getKey();
            ApiResponse response = entry.getValue();

            // 성공 응답 코드인 경우 (2xx)
            if (statusCode.startsWith("2") && response.getContent() != null) {
                response.getContent().forEach((mediaTypeKey, mediaType) -> {
                    Schema<?> originalSchema = mediaType.getSchema();
                    if (originalSchema != null) {
                        Schema<?> wrappedSchema = new ObjectSchema()
                                .addProperty("code", new IntegerSchema().example(ResCode.from(statusCode)))
                                .addProperty("message", new StringSchema().example("요청에 성공하였습니다"))
                                .addProperty("data", originalSchema);

                        mediaType.setSchema(wrappedSchema);
                    }
                });
            }
        }
    }

    private void addErrorSchema(Operation operation) {
        ApiResponse errorResponse = new ApiResponse()
                .description("잘못된 요청")
                .content(new Content().addMediaType("application/json", new MediaType().schema(
                        new ObjectSchema()
                                .addProperty("code", new IntegerSchema().example(ResCode.BAD_REQUEST_EXCEPTION))
                                .addProperty("message", new StringSchema().example("잘못된 요청입니다."))
                                .addProperty("data", new ObjectSchema().nullable(true))
                )));

        operation.getResponses().addApiResponse("400", errorResponse);
    }

}