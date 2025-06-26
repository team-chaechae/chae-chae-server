package com.project.chaechaeserver.presentation.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import com.project.chaechaeserver.infrastructure.security.CustomUserDetails;
import com.project.chaechaeserver.infrastructure.util.JwtUtil;
import com.project.chaechaeserver.presentation.request.user.ReqAuthPostLoginDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ReqAuthPostLoginDTO dto = new ObjectMapper().readValue(request.getInputStream(), ReqAuthPostLoginDTO.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getUser().getEmail(),
                            dto.getUser().getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) {
        String username = ((CustomUserDetails) authResult.getPrincipal()).getUsername();
        RoleType role = ((CustomUserDetails) authResult.getPrincipal()).getUser().getRole();

        String token = jwtUtil.generateAccessJwt(username, role);
        response.addHeader(JwtUtil.ACCESS_JWT_HEADER, token);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }

}