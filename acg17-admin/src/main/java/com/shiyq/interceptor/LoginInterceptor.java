package com.shiyq.interceptor;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import tools.jackson.databind.ObjectMapper;
import com.shiyq.constant.HttpStatus;
import com.shiyq.entity.DO.User;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.mapper.UserMapper;
import com.shiyq.util.JWTUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT拦截器，验证请求携带的token
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JWTUtil jwtUtil;
    private final UserMapper userMapper;

    public LoginInterceptor(JWTUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String msg;
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ") || token.length() <= 7) {
            msg = "Missing token! Please sign in.";
        } else {
            DecodedJWT decodedJWT;
            try {
                decodedJWT = jwtUtil.verify(token.substring(7));
            } catch (TokenExpiredException e) {
                msg = "Token expired! Please sign in again.";
                return unauthorized(response, msg);
            } catch (JWTVerificationException e) {
                msg = "Bad token! Please sign in again.";
                return unauthorized(response, msg);
            }

            Integer userId = decodedJWT.getClaim("userId").asInt();
            Integer authVersion = decodedJWT.getClaim("authVersion").asInt();
            if (userId == null || authVersion == null) {
                return unauthorized(response, "Bad token! Please sign in again.");
            }

            User user = userMapper.selectById(userId);
            if (user == null || !authVersion.equals(user.getAuthVersion())) {
                return unauthorized(response, "Session invalidated! Please sign in again.");
            }

            UserContext.add(userId);
            return true;
        }

        return unauthorized(response, msg);
    }

    private boolean unauthorized(HttpServletResponse response, String msg) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(ResultVO.error(HttpStatus.UNAUTHORIZED, msg));
        response.setStatus(HttpStatus.UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().println(json);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束，移除当前用户上下文
        UserContext.remove();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
