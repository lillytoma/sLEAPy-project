package com.sleapy.project.config;

import java.net.Authenticator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.sleapy.project.services.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter{
    private static final RequestMatcher PUBLIC_ENDPOINTS = request -> {
        String path = request.getServletPath();

        return "/api/auth/login".equals(path)
            || "/api/auth/signup".equals(path);
    };

    public JwtAuthenticationFilter(JwtService jwtService){
        super(new NegatedRequestMatcher(PUBLIC_ENDPOINTS));
        this.jwtService = jwtService;
    }

    private final JwtService jwtService;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException, IOException, ServletException {

        String bearerPrefix = "Bearer ";
        String header = httpServletRequest.getHeader("Authorization");


        if (header == null || !header.startsWith(bearerPrefix)) {

            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Please pass valid jwt token.");


        }else if(jwtService.verifyToken(header.substring(bearerPrefix.length()))==null){

            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "jwt token is invalid or incorrect");

        }
        else{

            String authenticationToken = header.substring(bearerPrefix.length());

            return getAuthenticationManager().authenticate(token);
        }

        return null;

    }
}
