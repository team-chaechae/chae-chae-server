package com.project.chaechaeserver.infrastructure.user.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.user.ResInternalUserPostSignupDTO;
import com.project.chaechaeserver.presentation.request.user.ReqInternalUserPostSignupDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "InternalUser", description = "회원가입 등 사내직원 관련 API를 제공합니다.")
@RequestMapping("/api/users")
public interface InternalUserControllerSwagger {

    @Operation(summary = "회원가입", description = "회원가입을 하는 API 입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = ResInternalUserPostSignupDTO.class))),
            @ApiResponse(responseCode = "400", description = "회원가입 실패.", content = @Content(schema = @Schema(implementation = ResInternalUserPostSignupDTO.class)))
    })
    @PostMapping("/signup")
    ResponseEntity<ResDTO<ResInternalUserPostSignupDTO>> signup(@RequestBody @Valid ReqInternalUserPostSignupDTO dto);
}
