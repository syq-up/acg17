package com.shiyq.controller;

import com.shiyq.constant.HttpStatus;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.LoginRequest;
import com.shiyq.entity.VO.LoginVO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.service.LoginAttemptLimiter;
import com.shiyq.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public UserController(UserService userService, LoginAttemptLimiter loginAttemptLimiter) {
        this.userService = userService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<ResultVO> login(@RequestBody @Valid LoginRequest loginRequest,
                                          HttpServletRequest request) {
        String attemptKey = request.getRemoteAddr() + ":"
                + loginRequest.getUsername().trim().toLowerCase(Locale.ROOT);
        if (loginAttemptLimiter.isBlocked(attemptKey)) {
            return ResponseEntity.status(429)
                    .body(ResultVO.error(429, "登录尝试过于频繁，请稍后再试。"));
        }

        LoginVO loginVO = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (loginVO == null) {
            loginAttemptLimiter.recordFailure(attemptKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResultVO.error(HttpStatus.UNAUTHORIZED, "用户名或密码错误！"));
        }

        loginAttemptLimiter.reset(attemptKey);
        return ResponseEntity.ok(ResultVO.success(loginVO));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResultVO> logout() {
        userService.logout(UserContext.getCurrentUserId());
        return ResponseEntity.ok(ResultVO.success());
    }

}
