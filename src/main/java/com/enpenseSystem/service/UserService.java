package com.enpenseSystem.service;

import com.enpenseSystem.dto.LoginRequest;
import com.enpenseSystem.dto.LoginResponse;
import com.enpenseSystem.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 用户认证服务接口。
 *
 * <p>定义登录、登出和获取当前登录用户的能力。</p>
 */
public interface UserService {

    /**
     * 用户登录。
     *
     * @param request  登录请求（用户名 + 密码）
     * @param response HTTP 响应，用于设置 Cookie
     * @return 登录成功后的用户信息
     */
    LoginResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * 用户登出。
     *
     * @param request  HTTP 请求，用于读取 Cookie
     * @param response HTTP 响应，用于清除 Cookie
     */
    void logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取当前登录用户。
     *
     * @param request HTTP 请求，用于读取 Cookie 中的 token
     * @return 当前用户实体，未登录返回 null
     */
    SysUser getCurrentUser(HttpServletRequest request);
}
