package com.project.chaechaeserver.application.service.user;

import com.project.chaechaeserver.application.response.user.ResInternalUserPostSignupDTO;
import com.project.chaechaeserver.presentation.request.user.ReqInternalUserPostSignupDTO;

public interface InternalUserService {

    ResInternalUserPostSignupDTO signup(ReqInternalUserPostSignupDTO dto);
}
