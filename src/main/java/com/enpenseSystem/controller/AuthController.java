package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.dto.LoginRequest;
import com.enpenseSystem.dto.LoginResponse;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.service.UserService;
import com.enpenseSystem.utils.ResultCodeConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证 HTTP 接口入口。
 *
 * <p>提供登录、登出和获取当前用户三个端点。
 * 所有端点在拦截器白名单中，不需要预先认证。</p>
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 用户登录，成功后在响应 Cookie 中写入 token。 */
    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginRequest request,
                        HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request, response);
        return Result.ok(loginResponse);
    }

    /** 用户登出，清除 Redis 中的 token 和客户端 Cookie。 */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request,
                         HttpServletResponse response) {
        userService.logout(request, response);
        return Result.ok();
    }

    /** 获取当前登录用户信息，用于页面刷新时恢复登录状态。 */
    @GetMapping("/current-user")
    public Result currentUser(HttpServletRequest request) {
        SysUser user = userService.getCurrentUser(request);
        if (user == null) {
            return Result.fail(ResultCodeConstants.UNAUTHORIZED_CODE,
                    ResultCodeConstants.UNAUTHORIZED_MESSAGE);
        }
        return Result.ok(new LoginResponse(user.getUsername(), user.getDisplayName()));
    }
}
