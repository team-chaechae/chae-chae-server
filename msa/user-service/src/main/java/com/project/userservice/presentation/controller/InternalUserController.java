package com.project.userservice.presentation.controller;

import com.project.userservice.application.global.constants.ResCode;
import com.project.userservice.application.global.dto.ResDTO;
import com.project.userservice.application.response.ResInternalUserPostSignupDTO;
import com.project.userservice.application.service.InternalUserService;
import com.project.userservice.presentation.controller.docs.InternalUserControllerSwagger;
import com.project.userservice.presentation.request.ReqInternalUserPostSignupDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class InternalUserController implements InternalUserControllerSwagger {

    private final InternalUserService internalUserService;

    @PostMapping("/signup")
    public ResponseEntity<ResDTO<ResInternalUserPostSignupDTO>> signup(@RequestBody @Valid ReqInternalUserPostSignupDTO dto) {

        return new ResponseEntity<>(
                ResDTO.<ResInternalUserPostSignupDTO>builder()
                        .code(ResCode.CREATED)
                        .message("회원가입에 성공하였습니다.")
                        .data(internalUserService.signup(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }
}
