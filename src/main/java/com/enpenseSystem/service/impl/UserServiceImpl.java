package com.enpenseSystem.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.dto.LoginRequest;
import com.enpenseSystem.dto.LoginResponse;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.mapper.SysUserMapper;
import com.enpenseSystem.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证服务实现。
 *
 * <p>使用 bcrypt 校验密码，UUID token 存入 Redis 并写入 HttpOnly Cookie。
 * Redis 不可用时降级放行认证（与整体系统策略一致）。</p>
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String TOKEN_COOKIE_NAME = "token";
    private static final String REDIS_KEY_PREFIX = "auth:token:";

    private final SysUserMapper sysUserMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${app.auth.token-ttl:86400}")
    private long tokenTtl;

    public UserServiceImpl(SysUserMapper sysUserMapper,
                           ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.sysUserMapper = sysUserMapper;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        // 1. 查用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 2. 校验启用状态
        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 3. bcrypt 校验密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 4. 生成 UUID token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 5. 写入 Redis（如果可用）
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY_PREFIX + token,
                        String.valueOf(user.getId()), tokenTtl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis写入token失败，降级放行: {}", e.getMessage());
            }
        }

        // 6. 设置 Cookie
        Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) tokenTtl);
        response.addCookie(cookie);

        return new LoginResponse(user.getUsername(), user.getDisplayName());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = getTokenFromCookie(request);

        if (token != null) {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            if (redis != null) {
                try {
                    redis.delete(REDIS_KEY_PREFIX + token);
                } catch (Exception e) {
                    log.warn("Redis删除token失败: {}", e.getMessage());
                }
            }
        }

        // 清除客户端 Cookie
        Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Override
    public SysUser getCurrentUser(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) {
            return null;
        }

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }

        try {
            String userId = redis.opsForValue().get(REDIS_KEY_PREFIX + token);
            if (userId == null) {
                return null;
            }
            return sysUserMapper.selectById(Long.valueOf(userId));
        } catch (Exception e) {
            log.warn("Redis读取token失败: {}", e.getMessage());
            return null;
        }
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
