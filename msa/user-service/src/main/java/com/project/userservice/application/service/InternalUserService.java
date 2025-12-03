package com.project.userservice.application.service;

import com.project.userservice.application.response.ResInternalUserPostSignupDTO;
import com.project.userservice.presentation.request.ReqInternalUserPostSignupDTO;

public interface InternalUserService {

    ResInternalUserPostSignupDTO signup(ReqInternalUserPostSignupDTO dto);
}
