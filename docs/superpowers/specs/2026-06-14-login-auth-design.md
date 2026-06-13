# 登录鉴权认证 — 设计规格

> 日期：2026-06-14 | 状态：待实现

## 1. 概述

为差旅报销系统新增基础登录鉴权认证功能。采用自定义 HandlerInterceptor + Token 方案，Cookie 存储，预置用户，仅校验登录状态不区分角色。

## 2. 需求决策

| 决策 | 选择 | 说明 |
|------|------|------|
| 认证方式 | 自定义 HandlerInterceptor + 简单 Token | 不引入 Spring Security，保持轻量 |
| 功能范围 | 仅登录 + 登出，预置用户 | 不含用户管理 CRUD |
| 权限粒度 | 仅校验登录状态 | 不区分角色 |
| Token 存储 | Cookie（HttpOnly） | 浏览器自动携带，前端无需手动处理 |
| 用户与员工 | 独立 | 用户只做登录账号，不关联报销单上的员工信息 |

## 3. 数据库变更

### 3.1 新建表 `sys_user`

```sql
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT 'bcrypt加密密码',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统用户表';

-- 预置管理员账号
-- 密码通过 Hutool SecureUtil.bcrypt("admin123") 生成，由应用启动时的 DataInitializer 写入
INSERT INTO sys_user (username, password, display_name) VALUES
('admin', '{bcrypt-generated-hash}', '系统管理员');
```

> `{bcrypt-generated-hash}` 是占位符，实际 hash 由初始化代码在应用启动时动态生成并写入。密码明文 `admin123`。

## 4. 后端设计

### 4.1 新增文件

```
com.enpenseSystem
├── entity/SysUser.java                # 用户实体（MyBatis-Plus）
├── mapper/SysUserMapper.java          # 用户 Mapper
├── service/UserService.java           # 用户服务接口
├── service/impl/UserServiceImpl.java  # 用户服务实现
├── dto/LoginRequest.java              # 登录请求 { username, password }
├── dto/LoginResponse.java             # 登录响应 { username, displayName }
├── controller/AuthController.java     # /api/auth/login & /api/auth/logout
├── config/AuthInterceptor.java        # 认证拦截器
└── config/WebMvcConfig.java           # 注册拦截器
```

### 4.2 修改文件

- **`ResultCodeConstants.java`** — 新增 `UNAUTHORIZED_CODE = 401`
- **`application.yaml`** — 新增 `app.auth.token-ttl: 86400`（可选配置）

### 4.3 实体：`SysUser.java`

```java
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### 4.4 DTO：`LoginRequest.java` / `LoginResponse.java`

```java
// LoginRequest
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}

// LoginResponse
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String username;
    private String displayName;
}
```

### 4.5 控制器：`AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request, response);
        return Result.ok(loginResponse);
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return Result.ok();
    }

    @GetMapping("/current-user")
    public Result currentUser(HttpServletRequest request) {
        // 从 Cookie 取 token，返回当前用户信息
        SysUser user = userService.getCurrentUser(request);
        if (user == null) {
            return Result.fail(ResultCodeConstants.UNAUTHORIZED_CODE, "未登录");
        }
        return Result.ok(new LoginResponse(user.getUsername(), user.getDisplayName()));
    }
}
```

### 4.6 服务：`UserServiceImpl` 核心逻辑

```
login():
  1. 根据 username 查询 sys_user
  2. 校验用户存在且 enabled = 1
  3. bcrypt 校验密码
  4. 生成随机 UUID 作为 token
  5. Redis SET auth:token:{uuid} = userId，TTL = 24h
  6. 设置 Cookie: token={uuid}; HttpOnly; Path=/; Max-Age=86400
  7. 返回 LoginResponse

logout():
  1. 从 Cookie 读取 token
  2. Redis DEL auth:token:{uuid}
  3. 清除 Cookie（Max-Age=0）

getCurrentUser():
  1. 从 Cookie 读取 token
  2. Redis GET auth:token:{uuid} 获取 userId
  3. 查询 sys_user 返回用户信息
```

### 4.7 拦截器：`AuthInterceptor.java`

```java
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> WHITE_LIST = List.of(
        "/api/auth/login",
        "/api/auth/logout"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 白名单放行
        String path = request.getRequestURI();
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return true;
        }

        // 从 Cookie 取 token
        String token = getTokenFromCookie(request);
        if (token == null) {
            writeUnauthorized(response);
            return false;
        }

        // Redis 校验
        try {
            String userId = stringRedisTemplate.opsForValue().get("auth:token:" + token);
            if (userId == null) {
                writeUnauthorized(response);
                return false;
            }
            // 存入 request attribute，供后续使用
            request.setAttribute("currentUserId", userId);
        } catch (Exception e) {
            // Redis 不可用时降级放行（保持现有行为，系统可用）
            return true;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
    }
}
```

> 关键设计：Redis 不可用时降级放行，保证系统可用性——与当前 Redis 降级策略一致。

### 4.8 配置：`WebMvcConfig.java`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(stringRedisTemplate))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/logout");
    }
}
```

### 4.9 密码加密

使用 Spring Boot 内置的 `BCryptPasswordEncoder`（`spring-boot-starter-security` 的子依赖，或者直接用 Hutool 的 `SecureUtil.bcrypt()`）。

考虑到当前项目已依赖 Hutool，初始版本使用 Hutool 的 bcrypt 方法，避免引入额外依赖：

```java
// 注册时加密密码
String hashed = SecureUtil.bcrypt(password);

// 登录时验证
boolean match = SecureUtil.bcryptCheck(password, user.getPassword());
```

## 5. 前端设计

### 5.1 新增文件

```
frontend/src
├── views/Login.vue          # 登录页面
├── api/auth.js              # 登录/登出 API
└── stores/auth.js           # Pinia 认证状态管理
```

### 5.2 修改文件

- **`router/index.js`** — 添加 `/login` 路由 + `beforeEach` 守卫
- **`api/request.js`** — 响应拦截器增加 401 → `/login` 跳转
- **`App.vue`** — 右侧顶部栏显示当前用户名 + 退出按钮

### 5.3 登录页面：`Login.vue`

```
布局:
┌──────────────────────────────────┐
│                                  │
│         🏷️ 差旅报销系统           │
│                                  │
│   ┌────────────────────────┐     │
│   │  用户名: [__________]  │     │
│   │  密码:   [__________]  │     │
│   │  [      登  录      ]  │     │
│   └────────────────────────┘     │
│                                  │
└──────────────────────────────────┘

- 全屏居中布局，卡片式表单
- el-card + el-form + el-input + el-button
- 风格对齐 ReimbursementForm.vue（Element Plus 组件，一致的表单校验风格）
- 登录按钮 loading 状态防重复提交
```

### 5.4 API：`auth.js`

```js
import request from './request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get('/auth/current-user')
}
```

> 风格对齐 `reimbursement.js`，直接使用统一的 `request` 实例。

### 5.5 Pinia Store：`stores/auth.js`

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)        // { username, displayName } | null
  const isLoggedIn = ref(false)

  async function login(username, password) {
    const data = await loginApi(username, password)
    user.value = data
    isLoggedIn.value = true
  }

  async function logout() {
    await logoutApi()
    user.value = null
    isLoggedIn.value = false
  }

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

### 5.6 路由守卫：`router/index.js`

```
beforeEach 逻辑:
  - 已登录(有 token cookie) → 放行
  - 未登录 + 目标不是 /login → 跳转 /login
  - 未登录 + 目标是 /login → 放行
  - 已登录 + 目标是 /login → 跳转 /

应用启动时:
  - App.vue onMounted 调用 authStore.checkAuth()
  - 验证当前 cookie 中的 token 是否有效
```

### 5.7 Axios 401 处理：`request.js`

在现有响应拦截器的错误处理中新增 401 判断：

```js
request.interceptors.response.use(
  (response) => { /* 现有逻辑不变 */ },
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token 过期或未登录，跳转登录页
      window.location.href = '/login'
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)
```

### 5.8 主布局：`App.vue` 修改

- 顶部栏右侧添加当前用户名显示（`el-dropdown` 样式）
- 下拉菜单：退出登录
- 未登录时不显示侧边栏和顶部栏（仅渲染 `<router-view />`）

## 6. 认证流程

```
┌─────────┐     ┌─────────┐     ┌──────────┐     ┌───────┐
│  Vue    │     │  Axios  │     │  Spring  │     │ Redis │
│  Login  │     │         │     │  Auth    │     │       │
└────┬────┘     └────┬────┘     └────┬─────┘     └───┬───┘
     │               │               │               │
     │ POST /api/auth/login          │               │
     │──────────────>│               │               │
     │               │──────────────>│               │
     │               │               │ 查sys_user    │
     │               │               │ bcrypt校验    │
     │               │               │ 生成UUID token│
     │               │               │──────────────>│ SET auth:token:uuid
     │               │               │<──────────────│ OK
     │               │ Set-Cookie    │               │
     │               │<──────────────│               │
     │<──────────────│               │               │
     │               │               │               │
     │ 后续请求 (Cookie 自动携带 token)              │
     │──────────────>│──────────────>│               │
     │               │               │──────────────>│ GET auth:token:uuid
     │               │               │<──────────────│ userId
     │               │               │ 放行          │
     │               │<──────────────│               │
     │<──────────────│               │               │
```

## 7. 错误处理与边界情况

| 场景 | 后端行为 | 前端行为 |
|------|----------|----------|
| 用户名不存在 | 返回 400 + "用户名或密码错误" | 表单下方显示错误提示 |
| 密码错误 | 返回 400 + "用户名或密码错误" | 表单下方显示错误提示 |
| 账号已禁用 | 返回 400 + "账号已被禁用" | 表单下方显示错误提示 |
| Token 过期 (24h) | 拦截器返回 401 | Axios 拦截器跳转 /login |
| Token 不存在 | 拦截器返回 401 | 导航守卫跳转 /login |
| Redis 不可用 | 拦截器降级放行 | 正常访问（与现有行为一致） |
| 登录表单为空 | `@Valid` 校验 → 400 | el-form rules 前端校验 |
| 重复提交登录 | — | 按钮 loading 状态防重复 |

## 8. 依赖变更

**不新增任何 Maven/NPM 依赖。** bcrypt 使用 Hutool（已有），不引入 Spring Security。

## 9. 测试要点

- 正确用户名+密码 → 登录成功，跳转首页
- 错误密码 → 提示"用户名或密码错误"
- 登录后刷新页面 → 保持登录状态
- 24h 后 token 过期 → API 返回 401，跳转登录页
- 登出 → Cookie 清除，跳转登录页
- 未登录直接访问 URL → 跳转登录页
- 登录后访问 /login → 重定向到 /
- Redis 宕机 → 系统仍可用（降级放行）
