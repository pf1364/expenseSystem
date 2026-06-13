package com.enpenseSystem.config;

import com.enpenseSystem.utils.ResultCodeConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 登录认证拦截器。
 *
 * <p>拦截 /api/** 路径，从 Cookie 中提取 token 并在 Redis 中校验。
 * /api/auth/login 和 /api/auth/logout 在白名单中直接放行。
 * Redis 不可用时降级放行，保证系统可用性。</p>
 */
public class AuthInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String TOKEN_COOKIE_NAME = "token";
    private static final String REDIS_KEY_PREFIX = "auth:token:";

    /** 不需要认证的白名单路径前缀。 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/logout"
    );

    private final StringRedisTemplate stringRedisTemplate;

    public AuthInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 白名单放行
        String path = request.getRequestURI();
        for (String white : WHITE_LIST) {
            if (path.startsWith(white)) {
                return true;
            }
        }

        // 从 Cookie 取 token
        String token = getTokenFromCookie(request);
        if (token == null) {
            writeUnauthorized(response, "未登录");
            return false;
        }

        // Redis 校验
        try {
            String userId = stringRedisTemplate.opsForValue()
                    .get(REDIS_KEY_PREFIX + token);
            if (userId == null) {
                writeUnauthorized(response, "登录已过期");
                return false;
            }
            // 存入 request attribute，供控制器使用
            request.setAttribute("currentUserId", userId);
        } catch (Exception e) {
            // Redis 不可用时降级放行
            log.warn("Redis不可用，认证降级放行: {}", e.getMessage());
        }
        return true;
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

    private void writeUnauthorized(HttpServletResponse response,
                                   String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                        ResultCodeConstants.UNAUTHORIZED_CODE, message));
    }
}
