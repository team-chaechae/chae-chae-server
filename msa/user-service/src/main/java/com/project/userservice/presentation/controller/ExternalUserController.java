package com.project.userservice.presentation.controller;

import com.project.userservice.application.global.constants.ResCode;
import com.project.userservice.application.global.dto.ResDTO;
import com.project.userservice.application.response.ResExternalUserDTO;
import com.project.userservice.application.response.ResExternalUserSignupDTO;
import com.project.userservice.application.service.ExternalUserService;
import com.project.userservice.presentation.controller.docs.ExternalUserControllerSwagger;
import com.project.userservice.presentation.request.ReqExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/customers")
public class ExternalUserController implements ExternalUserControllerSwagger {

    private final ExternalUserService externalUserService;

    @Override
    @PostMapping("/signup")
    public ResponseEntity<ResDTO<ResExternalUserSignupDTO>> signup(@Valid @RequestBody ReqExternalUserSignupDTO dto) {
        return new ResponseEntity<>(
                ResDTO.<ResExternalUserSignupDTO>builder()
                        .code(ResCode.CREATED)
                        .message("고객 회원가입에 성공하였습니다.")
                        .data(externalUserService.signup(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ResDTO<ResExternalUserDTO>> getMyInfo(@RequestHeader("User-Id") String email) {
        return new ResponseEntity<>(
                ResDTO.<ResExternalUserDTO>builder()
                        .code(ResCode.OK)
                        .message("내 정보 조회에 성공하였습니다.")
                        .data(externalUserService.getMyInfo(email))
                        .build(),
                HttpStatus.OK
        );
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<ResDTO<ResExternalUserDTO>> updateMyInfo(
            @RequestHeader("User-Id") String email,
            @Valid @RequestBody ReqExternalUserUpdateDTO dto
    ) {
        return new ResponseEntity<>(
                ResDTO.<ResExternalUserDTO>builder()
                        .code(ResCode.OK)
                        .message("내 정보 수정에 성공하였습니다.")
                        .data(externalUserService.updateMyInfo(email, dto))
                        .build(),
                HttpStatus.OK
        );
    }
}
