package com.project.chaechaeserver.presentation.controller.user;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.service.user.InternalUserService;
import com.project.chaechaeserver.application.response.user.ResInternalUserPostSignupDTO;
import com.project.chaechaeserver.presentation.request.user.ReqInternalUserPostSignupDTO;
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
public class InternalUserController {

    private final InternalUserService internalUserService;

    @PostMapping
    public ResponseEntity<ResDTO<ResInternalUserPostSignupDTO>> signup(@RequestBody @Valid ReqInternalUserPostSignupDTO dto) {

        return new ResponseEntity<>(
                ResDTO.<ResInternalUserPostSignupDTO>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("회원가입에 성공하였습니다.")
                        .data(internalUserService.signup(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }
}