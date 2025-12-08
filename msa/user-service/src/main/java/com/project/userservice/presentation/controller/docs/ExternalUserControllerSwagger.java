package com.project.userservice.presentation.controller.docs;

import com.project.userservice.application.global.dto.ResDTO;
import com.project.userservice.application.response.ResExternalUserDTO;
import com.project.userservice.application.response.ResExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "External User (Customer)", description = "고객 회원 관련 API")
@RequestMapping("/api/users/customers")
public interface ExternalUserControllerSwagger {

    @Operation(summary = "고객 회원가입", description = "새로운 고객 계정을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = ResExternalUserSignupDTO.class))),
            @ApiResponse(responseCode = "400", description = "회원가입 실패 (이메일 중복 등)",
                    content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping("/signup")
    ResponseEntity<ResDTO<ResExternalUserSignupDTO>> signup(@Valid @RequestBody ReqExternalUserSignupDTO dto);

    @Operation(summary = "내 정보 조회", description = "로그인한 고객의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ResExternalUserDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping("/me")
    ResponseEntity<ResDTO<ResExternalUserDTO>> getMyInfo(@RequestHeader("User-Id") String email);

    @Operation(summary = "내 정보 수정", description = "로그인한 고객의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ResExternalUserDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/me")
    ResponseEntity<ResDTO<ResExternalUserDTO>> updateMyInfo(
            @RequestHeader("User-Id") String email,
            @Valid @RequestBody ReqExternalUserUpdateDTO dto
    );
}
