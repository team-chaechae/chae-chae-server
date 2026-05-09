package com.project.userservice.application.service;

import com.project.userservice.application.response.ResExternalUserDTO;
import com.project.userservice.application.response.ResExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserUpdateDTO;

public interface ExternalUserService {

    ResExternalUserSignupDTO signup(ReqExternalUserSignupDTO dto);

    ResExternalUserDTO getMyInfo(String email);

    ResExternalUserDTO updateMyInfo(String email, ReqExternalUserUpdateDTO dto);
}
