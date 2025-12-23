package com.project.userservice.application.service;

import com.project.userservice.application.response.ResAuthLoginDTO;
import com.project.userservice.presentation.request.ReqAuthPostLoginDTO;

public interface AuthService {
    ResAuthLoginDTO login(ReqAuthPostLoginDTO request);
    void logout(String token);
    ResAuthLoginDTO refresh(String refreshToken);
}
