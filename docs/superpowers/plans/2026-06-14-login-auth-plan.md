# 登录鉴权认证 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为差旅报销系统新增基于 Token+Cookie 的基础登录/登出认证功能，预置管理员用户。

**Architecture:** 后端用 Spring HandlerInterceptor 拦截 `/api/**` 请求，从 Cookie 读取 token 并在 Redis 校验；前端新增 Login 页面和 Pinia auth store，路由守卫 + Axios 401 拦截驱动登录跳转。不引入 Spring Security。

**Tech Stack:** Spring Boot 3.2.10, MyBatis-Plus 3.5.9, Hutool bcrypt, Redis (StringRedisTemplate), Vue 3 + Pinia + Element Plus + Vue Router 4, Cookie (HttpOnly)

**Spec:** `docs/superpowers/specs/2026-06-14-login-auth-design.md`

---

## 文件清单

### 后端新增（9 个文件）
| 文件 | 职责 |
|------|------|
| `entity/SysUser.java` | 用户实体，映射 `sys_user` 表 |
| `mapper/SysUserMapper.java` | MyBatis-Plus Mapper |
| `dto/LoginRequest.java` | 登录请求 `{ username, password }` |
| `dto/LoginResponse.java` | 登录响应 `{ username, displayName }` |
| `service/UserService.java` | 用户服务接口 |
| `service/impl/UserServiceImpl.java` | 用户服务实现：登录校验、登出、获取当前用户 |
| `controller/AuthController.java` | `/api/auth/login`、`/api/auth/logout`、`/api/auth/current-user` |
| `config/AuthInterceptor.java` | 认证拦截器：Cookie token → Redis 校验 |
| `config/DataInitializer.java` | 启动时初始化管理员账号 |

### 后端修改（3 个文件）
| 文件 | 变更 |
|------|------|
| `utils/ResultCodeConstants.java` | 新增 `UNAUTHORIZED_CODE = 401` |
| `config/WebMvcConfig.java` | 新建，注册 AuthInterceptor |
| `src/main/resources/application.yaml` | 新增 `app.auth.token-ttl` 配置 |

### 前端新增（3 个文件）
| 文件 | 职责 |
|------|------|
| `api/auth.js` | 登录/登出/获取当前用户的 API 封装 |
| `stores/auth.js` | Pinia 认证状态管理（Composition API） |
| `views/Login.vue` | 登录页面 |

### 前端修改（3 个文件）
| 文件 | 变更 |
|------|------|
| `router/index.js` | 添加 `/login` 路由 + `beforeEach` 导航守卫 |
| `api/request.js` | 响应错误处理增加 401 → `/login` 跳转 |
| `App.vue` | 顶部栏显示用户名 + 退出按钮；未登录时隐藏侧边栏 |

---

### Task 1: 基础准备 — SQL 建表脚本 + 响应码 + 配置

**Files:**
- Create: `src/main/resources/sql/sys_user.sql`
- Modify: `src/main/java/com/enpenseSystem/utils/ResultCodeConstants.java`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: 创建 SQL 建表脚本**

创建 `src/main/resources/sql/sys_user.sql`：

```sql
-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT 'bcrypt加密密码',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统用户表';
```

> 注：表结构创建后由 DataInitializer 在应用启动时自动写入预置用户，无需手动 INSERT。

- [ ] **Step 2: 新增 401 响应码**

在 `ResultCodeConstants.java` 的 `NOT_FOUND_CODE` 之前添加：

```java
public static final Integer UNAUTHORIZED_CODE = 401;
public static final String UNAUTHORIZED_MESSAGE = "未登录或登录已过期";
```

完整文件修改：在 `PARAM_ERROR_CODE` 和 `NOT_FOUND_CODE` 之间插入以上两行。

- [ ] **Step 3: 新增 auth 配置**

在 `application.yaml` 末尾的 `app:` 块中新增：

```yaml
  auth:
    token-ttl: ${APP_AUTH_TOKEN_TTL:86400}
```

完整 `app:` 块变为：

```yaml
app:
  kafka:
    enabled: ${APP_KAFKA_ENABLED:false}
  auth:
    token-ttl: ${APP_AUTH_TOKEN_TTL:86400}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/resources/sql/sys_user.sql src/main/java/com/enpenseSystem/utils/ResultCodeConstants.java src/main/resources/application.yaml
git commit -m "feat: SQL建表脚本、401响应码和auth配置"
```

---

### Task 2: SysUser 实体 + Mapper

**Files:**
- Create: `src/main/java/com/enpenseSystem/entity/SysUser.java`
- Create: `src/main/java/com/enpenseSystem/mapper/SysUserMapper.java`

- [ ] **Step 1: 创建 SysUser 实体**

创建 `src/main/java/com/enpenseSystem/entity/SysUser.java`：

```java
package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体，对应数据库表 sys_user。
 *
 * <p>用户只用于登录认证，与报销单上的报销人/出差人无关。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 SysUserMapper**

创建 `src/main/java/com/enpenseSystem/mapper/SysUserMapper.java`：

```java
package com.enpenseSystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enpenseSystem.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 系统用户 Mapper，继承 MyBatis-Plus 基础增删改查能力。 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

> 注：现有 Mapper 没有 `@Mapper` 注解（依赖 `@MapperScan`），但新加一个显式 `@Mapper` 也无副作用，保持一致性用 `@Mapper` 或省略均可。此处加上以显式声明。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/enpenseSystem/entity/SysUser.java src/main/java/com/enpenseSystem/mapper/SysUserMapper.java
git commit -m "feat: SysUser实体和Mapper"
```

---

### Task 3: LoginRequest / LoginResponse DTO

**Files:**
- Create: `src/main/java/com/enpenseSystem/dto/LoginRequest.java`
- Create: `src/main/java/com/enpenseSystem/dto/LoginResponse.java`

- [ ] **Step 1: 创建 LoginRequest**

创建 `src/main/java/com/enpenseSystem/dto/LoginRequest.java`：

```java
package com.enpenseSystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建 LoginResponse**

创建 `src/main/java/com/enpenseSystem/dto/LoginResponse.java`：

```java
package com.enpenseSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应体，返回当前登录用户的基本信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String displayName;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/enpenseSystem/dto/LoginRequest.java src/main/java/com/enpenseSystem/dto/LoginResponse.java
git commit -m "feat: 登录请求和响应DTO"
```

---

### Task 4: UserService 接口 + UserServiceImpl 实现

**Files:**
- Create: `src/main/java/com/enpenseSystem/service/UserService.java`
- Create: `src/main/java/com/enpenseSystem/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 创建 UserService 接口**

创建 `src/main/java/com/enpenseSystem/service/UserService.java`：

```java
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
```

- [ ] **Step 2: 创建 UserServiceImpl 实现**

创建 `src/main/java/com/enpenseSystem/service/impl/UserServiceImpl.java`：

```java
package com.enpenseSystem.service.impl;

import cn.hutool.crypto.SecureUtil;
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
import java.util.Optional;
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
        if (!SecureUtil.bcryptCheck(request.getPassword(), user.getPassword())) {
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
        // 从 Cookie 读取 token
        String token = getTokenFromCookie(request);

        // 从 Redis 删除 token
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

    /**
     * 从请求的 Cookie 中提取 token 值。
     */
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
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/enpenseSystem/service/UserService.java src/main/java/com/enpenseSystem/service/impl/UserServiceImpl.java
git commit -m "feat: UserService登录认证服务实现"
```

---

### Task 5: DataInitializer — 预置管理员用户

**Files:**
- Create: `src/main/java/com/enpenseSystem/config/DataInitializer.java`

- [ ] **Step 1: 创建 DataInitializer**

创建 `src/main/java/com/enpenseSystem/config/DataInitializer.java`：

```java
package com.enpenseSystem.config;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的数据初始化。
 *
 * <p>首次启动时自动创建预置管理员账号（用户名: admin，密码: admin123）。
 * 如果管理员已存在则跳过，不重复创建。</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_ADMIN = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final SysUserMapper sysUserMapper;

    public DataInitializer(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, DEFAULT_ADMIN));
        if (count != null && count > 0) {
            log.info("管理员用户已存在，跳过初始化");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(DEFAULT_ADMIN);
        admin.setPassword(SecureUtil.bcrypt(DEFAULT_PASSWORD));
        admin.setDisplayName("系统管理员");
        admin.setEnabled(true);
        sysUserMapper.insert(admin);

        log.info("已创建预置管理员用户: admin / admin123");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/enpenseSystem/config/DataInitializer.java
git commit -m "feat: 启动时预置管理员用户"
```

---

### Task 6: AuthController

**Files:**
- Create: `src/main/java/com/enpenseSystem/controller/AuthController.java`

- [ ] **Step 1: 创建 AuthController**

创建 `src/main/java/com/enpenseSystem/controller/AuthController.java`：

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/enpenseSystem/controller/AuthController.java
git commit -m "feat: AuthController登录/登出/当前用户接口"
```

---

### Task 7: AuthInterceptor + WebMvcConfig

**Files:**
- Create: `src/main/java/com/enpenseSystem/config/AuthInterceptor.java`
- Create: `src/main/java/com/enpenseSystem/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 AuthInterceptor**

创建 `src/main/java/com/enpenseSystem/config/AuthInterceptor.java`：

```java
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
        response.setStatus(200); // 走业务码 401，HTTP 状态仍为 200
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                        ResultCodeConstants.UNAUTHORIZED_CODE, message));
    }
}
```

> 关键设计：拦截器返回 200 HTTP 状态 + 业务码 401，与现有前端 Axios 拦截器的处理逻辑对齐（`error.response.status === 401` → 需要修改前端判断逻辑，见 Task 12）。另一种方案是直接返回 HTTP 401，让 Axios 的 error 分支处理。这里选用 HTTP 401 方案——修改如下：

**修正 `writeUnauthorized` 方法，返回 HTTP 401：**

```java
private void writeUnauthorized(HttpServletResponse response,
                               String message) throws IOException {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
            String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    ResultCodeConstants.UNAUTHORIZED_CODE, message));
}
```

> 这样前端 Axios 响应拦截器的 error 分支可以直接通过 `error.response.status === 401` 判断。

- [ ] **Step 2: 创建 WebMvcConfig**

创建 `src/main/java/com/enpenseSystem/config/WebMvcConfig.java`：

```java
package com.enpenseSystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 *
 * <p>注册认证拦截器，拦截 /api/** 路径，放行登录和登出端点。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;

    public WebMvcConfig(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(stringRedisTemplate))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/logout");
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/enpenseSystem/config/AuthInterceptor.java src/main/java/com/enpenseSystem/config/WebMvcConfig.java
git commit -m "feat: AuthInterceptor认证拦截器和WebMvcConfig"
```

---

### Task 8: 前端 — auth.js API 封装

**Files:**
- Create: `frontend/src/api/auth.js`

- [ ] **Step 1: 创建 auth.js**

创建 `frontend/src/api/auth.js`：

```js
import request from './request'

// 认证相关 API 封装，风格对齐 reimbursement.js。

/** 登录 */
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

/** 登出 */
export function logout() {
  return request.post('/auth/logout')
}

/** 获取当前登录用户信息 */
export function getCurrentUser() {
  return request.get('/auth/current-user')
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/auth.js
git commit -m "feat: 前端auth API封装"
```

---

### Task 9: 前端 — Pinia auth Store

**Files:**
- Create: `frontend/src/stores/auth.js`

- [ ] **Step 1: 创建 auth store**

创建 `frontend/src/stores/auth.js`：

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

/**
 * 认证状态管理。
 *
 * 使用 Composition API（setup 写法），
 * 存储当前用户信息和登录状态，暴露 login/logout/checkAuth 三个 action。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)        // { username, displayName } | null
  const isLoggedIn = ref(false)

  /** 登录并更新状态 */
  async function login(username, password) {
    const data = await loginApi(username, password)
    user.value = data
    isLoggedIn.value = true
  }

  /** 登出并清除状态 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      user.value = null
      isLoggedIn.value = false
    }
  }

  /** 应用启动/刷新时检查登录状态 */
  async function checkAuth() {
    try {
      const data = await getCurrentUser()
      user.value = data
      isLoggedIn.value = true
    } catch {
      user.value = null
      isLoggedIn.value = false
    }
  }

  return { user, isLoggedIn, login, logout, checkAuth }
})
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/stores/auth.js
git commit -m "feat: Pinia auth store"
```

---

### Task 10: 前端 — Login.vue 登录页面

**Files:**
- Create: `frontend/src/views/Login.vue`

- [ ] **Step 1: 创建 Login.vue**

创建 `frontend/src/views/Login.vue`：

```vue
<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-brand-mark">差</div>
        <div>
          <div class="login-brand-title">差旅报销系统</div>
          <div class="login-brand-subtitle">Travel Expense Reimbursement</div>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登  录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f4f7fb;
}

.login-card {
  width: 400px;
  padding: 44px 40px 32px;
  background: #fff;
  border: 1px solid #e5ebf1;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
  justify-content: center;
}

.login-brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #1f9d8a;
  color: #fff;
  font-weight: 700;
  font-size: 20px;
}

.login-brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #17202a;
}

.login-brand-subtitle {
  margin-top: 2px;
  color: #9fb0bf;
  font-size: 12px;
}

.login-form {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/Login.vue
git commit -m "feat: Login登录页面"
```

---

### Task 11: 前端 — 路由新增登录页 + 导航守卫

**Files:**
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 更新路由配置**

将 `frontend/src/router/index.js` 修改为：

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ReimbursementList from '../views/ReimbursementList.vue'
import ReimbursementForm from '../views/ReimbursementForm.vue'
import PersonalStats from '../views/PersonalStats.vue'
import Login from '../views/Login.vue'

const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/login', component: Login, meta: { guest: true } },
  { path: '/reimbursements', component: ReimbursementList, meta: { requiresAuth: true } },
  { path: '/reimbursements/new', component: ReimbursementForm, meta: { requiresAuth: true } },
  { path: '/reimbursements/:reimNo', component: ReimbursementForm, props: true, meta: { requiresAuth: true } },
  { path: '/statistics/personal', component: PersonalStats, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 首次进入时需要检查登录状态（Pinia 状态不持久化，刷新丢失）
  if (!authStore.isLoggedIn) {
    await authStore.checkAuth()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    // 需要登录但未登录 → 跳转登录页
    next('/login')
  } else if (to.meta.guest && authStore.isLoggedIn) {
    // 已登录访问登录页 → 跳转首页
    next('/')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/router/index.js
git commit -m "feat: 路由增加登录页和导航守卫"
```

---

### Task 12: 前端 — Axios 401 拦截

**Files:**
- Modify: `frontend/src/api/request.js`

- [ ] **Step 1: 更新响应拦截器**

在 `request.js` 的 error 分支中增加 401 处理。将现有的错误处理：

```js
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message
    if (status === 400) {
      ElMessage.error(msg || '参数错误')
    } else if (status === 404) {
      ElMessage.error(msg || '资源不存在')
    } else if (status === 409) {
      ElMessage.warning(msg || '数据冲突，请刷新后重试')
    } else {
      ElMessage.error('网络异常')
    }
    return Promise.reject(error)
  }
```

修改为：

```js
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message

    if (status === 401) {
      // Token 过期或未登录，跳转登录页
      window.location.href = '/login'
      return Promise.reject(error)
    }
    if (status === 400) {
      ElMessage.error(msg || '参数错误')
    } else if (status === 404) {
      ElMessage.error(msg || '资源不存在')
    } else if (status === 409) {
      ElMessage.warning(msg || '数据冲突，请刷新后重试')
    } else {
      ElMessage.error('网络异常')
    }
    return Promise.reject(error)
  }
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/request.js
git commit -m "feat: Axios响应拦截器增加401跳转登录页"
```

---

### Task 13: 前端 — App.vue 增加用户信息 + 退出按钮

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: 更新 App.vue**

将 `App.vue` 修改为：

```vue
<template>
  <el-container class="app-shell">
    <!-- 未登录时不显示侧边栏 -->
    <el-aside v-if="authStore.isLoggedIn" width="232px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">差</div>
        <div>
          <div class="brand-title">差旅报销</div>
          <div class="brand-subtitle">Travel Expense</div>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/reimbursements">
          <el-icon><Document /></el-icon>
          <span>报销单主界面</span>
        </el-menu-item>
        <el-menu-item index="/statistics/personal">
          <el-icon><TrendCharts /></el-icon>
          <span>个人信息统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 登录页面不显示外层容器，由 Login.vue 自己渲染 -->
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <el-container v-else>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">{{ pageTitle }}</div>
          <div class="topbar-subtitle">{{ pageSubtitle }}</div>
        </div>
        <!-- 右侧用户信息 + 退出 -->
        <div v-if="authStore.isLoggedIn" class="topbar-user">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="user-name">{{ authStore.user?.displayName || authStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, TrendCharts, UserFilled, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  // 应用启动时检查登录状态
  if (!authStore.isLoggedIn) {
    authStore.checkAuth()
  }
})

async function handleCommand(command) {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}

const pageTitle = computed(() => {
  if (route.path.startsWith('/statistics')) return '个人信息统计'
  if (route.path.includes('/new')) return '新建差旅报销单'
  if (route.params.reimNo) return '报销单详情'
  return '报销单主界面'
})

const pageSubtitle = computed(() => {
  if (route.path.startsWith('/statistics')) return '按报销人查看金额趋势和费用归属结构'
  if (route.path.includes('/new') || route.params.reimNo) return '维护基础信息、行程、每日补助和费用分摊'
  return '查询、创建、提交、删除和作废差旅报销单'
})
</script>

<style scoped>
.topbar-user {
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #465460;
  font-size: 14px;
}

.user-info:hover {
  color: #1f9d8a;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/App.vue
git commit -m "feat: App.vue增加用户信息和退出登录按钮"
```

---

### Task 14: 验证测试

- [ ] **Step 1: 执行建表 SQL**

在 MySQL `enpense_system` 数据库中执行 `src/main/resources/sql/sys_user.sql`。

- [ ] **Step 2: 启动后端**

```bash
./mvnw spring-boot:run
```

预期：控制台输出 `已创建预置管理员用户: admin / admin123`（首次启动）。

- [ ] **Step 3: 手动测试 — 未登录访问 API**

```bash
curl -s http://localhost:8081/api/reimbursements/page
```

预期返回：`{"code":401,"message":"未登录","data":null}`（HTTP 状态 401）。

- [ ] **Step 4: 手动测试 — 登录**

```bash
curl -v -s -c - http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

预期返回：`{"code":200,"message":"success","data":{"username":"admin","displayName":"系统管理员"}}`，并在 Cookie 中带有 `token=<uuid>`。

- [ ] **Step 5: 手动测试 — 带 Cookie 访问 API**

```bash
# 先用上一步获取的 token
curl -s http://localhost:8081/api/reimbursements/page \
  -H "Cookie: token=<从上一步获取的token>"
```

预期返回分页数据（正常业务响应）。

- [ ] **Step 6: 手动测试 — 登出**

```bash
curl -s http://localhost:8081/api/auth/logout \
  -X POST \
  -H "Cookie: token=<token>"
```

预期返回成功。再用同样的 token 访问 `/api/reimbursements/page` 应返回 401。

- [ ] **Step 7: 启动前端**

```bash
cd frontend && npm run dev
```

- [ ] **Step 8: 浏览器手动测试**

1. 访问 `http://localhost:5173` → 应自动跳转到 `/login`
2. 输入用户名 `admin`，密码 `admin123` → 登录成功，跳转首页
3. 刷新页面 → 保持登录状态
4. 点击右上角用户名 → 退出登录 → 跳转回登录页
5. 登录后关闭标签页重新打开 → Cookie 持久化，应在有效期内保持登录
6. 直接访问 `http://localhost:5173/login` → 已登录状态下应重定向到 `/`
```

## 计划自查

**1. Spec 覆盖：**
- 数据库 sys_user 表 → Task 1
- SysUser 实体 + Mapper → Task 2
- LoginRequest/LoginResponse DTO → Task 3
- UserService + UserServiceImpl → Task 4
- DataInitializer 预置用户 → Task 5
- AuthController → Task 6
- AuthInterceptor + WebMvcConfig → Task 7
- 前端 auth API → Task 8
- Pinia auth store → Task 9
- Login.vue → Task 10
- Router 登录路由 + beforeEach 守卫 → Task 11
- Axios 401 拦截 → Task 12
- App.vue 用户信息 + 退出按钮 → Task 13
- 验证测试 → Task 14

**2. 无占位符：** 所有代码块完整无 TODO/TBD。

**3. 类型一致性：**
- `LoginResponse(username, displayName)` — 所有后端返回和前端消费统一使用这两个字段
- Cookie 名称统一为 `token`
- Redis key 前缀统一为 `auth:token:`
- Pinia store 中 `user.value` 结构为 `{ username, displayName }`
