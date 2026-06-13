# 登录鉴权认证 — 设计规格

> 日期：2026-06-14 | 状态：**已实现**

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

> 建表后启动应用，`DataInitializer` 会自动创建预置管理员（用户名 `admin`，密码 `admin123`，bcrypt 加密存储）。
>
> **注意**：不要手动 INSERT 用户，密码必须通过 bcrypt 加密。让 `DataInitializer` 处理。

## 4. 后端设计

### 4.1 新增文件

```
com.enpenseSystem
├── entity/SysUser.java                # 用户实体（MyBatis-Plus）
├── mapper/SysUserMapper.java          # 用户 Mapper
├── service/UserService.java           # 用户服务接口
├── service/impl/UserServiceImpl.java  # 用户服务实现（bcrypt + Redis token + Cookie）
├── dto/LoginRequest.java              # 登录请求 { username, password }
├── dto/LoginResponse.java             # 登录响应 { username, displayName }
├── controller/AuthController.java     # /api/auth/login, /api/auth/logout, /api/auth/current-user
├── config/AuthInterceptor.java        # 认证拦截器（Cookie token → Redis 校验）
├── config/WebMvcConfig.java           # 注册拦截器
└── config/DataInitializer.java        # 启动时预置管理员账号
```

### 4.2 修改文件

- **`ResultCodeConstants.java`** — 新增 `UNAUTHORIZED_CODE = 401`
- **`application.yaml`** — 新增 `app.auth.token-ttl: ${APP_AUTH_TOKEN_TTL:86400}`

### 4.3 关键实现细节

- **密码加密**：使用 Hutool `BCrypt.hashpw()` / `BCrypt.checkpw()`，不引入 Spring Security
- **Redis 降级**：`ObjectProvider<StringRedisTemplate>` 模式——Redis 不可用时认证降级放行，与现有系统策略一致
- **拦截器白名单**：`/api/auth/login`、`/api/auth/logout`、`/api/auth/current-user`（`current-user` 必须在白名单，由 Controller 自行判断登录态，否则前端 `checkAuth()` 会触发死循环）
- **构造函数注入**：所有依赖通过构造函数注入，不使用 `@Autowired` 字段

## 5. 前端设计

### 5.1 新增文件

```
frontend/src
├── views/Login.vue          # 登录页面（Element Plus 卡片式居中布局）
├── api/auth.js              # 登录/登出 API 封装
└── stores/auth.js           # Pinia 认证状态管理（Composition API）
```

### 5.2 修改文件

- **`router/index.js`** — 添加 `/login` 路由（`meta: { guest: true }`）+ `beforeEach` 守卫，仅 `requiresAuth` 页面触发认证检查
- **`api/request.js`** — 响应拦截器增加 `status === 401` → `window.location.href = '/login'`
- **`App.vue`** — 用 `authStore.isLoggedIn` 控制布局：未登录显示裸露 `<router-view />`，已登录显示完整壳子（侧边栏 + 顶栏用户信息 + 退出按钮）
- **`vite.config.js`** — 添加 `resolve.alias`：`@` → `./src`（解决 `@/stores/auth` 等导入）

### 5.3 关键实现细节

- **`checkAuth()` 逻辑**：API 返回 `null`（未登录）时必须设 `isLoggedIn = false`，否则会导致未登录时错误显示主界面壳子
- **Axios 响应解包**：`request.js` 成功拦截器返回 `response.data?.data`，`/api/auth/current-user` 未登录时返回 `{code:401, data:null}` → 解包得到 `null`
- **登录框居中**：`.login-page` 必须设 `width: 100%`，因为 `el-container` 是 flex 容器，子元素不会自动撑满

## 6. 认证流程

```
未登录访问 / → redirect /reimbursements → beforeEach guard
  → requiresAuth && !isLoggedIn → checkAuth()
    → GET /api/auth/current-user → 返回 null
    → isLoggedIn 保持 false → next('/login')
      → Login.vue 渲染（居中卡片，无侧边栏/顶栏）

输入 admin / admin123 → POST /api/auth/login
  → bcrypt 校验 → UUID token 写入 Redis + HttpOnly Cookie
  → 返回 { username, displayName }
  → authStore.login() → isLoggedIn = true
  → router.push('/') → 显示完整主界面

后续请求：浏览器自动携带 Cookie → AuthInterceptor 从 Redis 校验 token

登出 → POST /api/auth/logout → Redis 删除 token → Cookie 清除
  → isLoggedIn = false → 跳转登录页
```

## 7. 部署与执行步骤

### 首次部署

1. 在 MySQL `enpense_system` 库中执行建表 SQL（见第 3 节）
2. 启动后端：`mvn spring-boot:run`（`DataInitializer` 自动创建 admin 用户，日志输出 `已创建预置管理员用户: admin / admin123`）
3. 启动前端：`cd frontend && npm run dev`
4. 访问 `http://localhost:5173`，使用 `admin` / `admin123` 登录

### 后续启动

- 后端：`mvn spring-boot:run`（`DataInitializer` 检测到 admin 已存在则跳过）
- 前端：`cd frontend && npm run dev`
- 预置账号：`admin` / `admin123`

### 配置

- Token 有效期：`application.yaml` 中 `app.auth.token-ttl`，默认 86400 秒（24 小时）
- 环境变量覆盖：`APP_AUTH_TOKEN_TTL=3600`

## 8. 实现过程中发现并修复的问题

| # | 问题 | 修复 |
|---|------|------|
| 1 | `application.yaml` 泄漏个人环境配置（数据库 IP、密码） | 恢复为通用默认值 `127.0.0.1` / 空密码 |
| 2 | `BCrypt.hashpw()` 单参数 vs 双参数 | Hutool 5.8.27 中确认可用单参数自动生成 salt |
| 3 | Vite 缺少 `@` 别名 → `@/stores/auth` 无法解析 | `vite.config.js` 添加 `resolve.alias` |
| 4 | `/api/auth/current-user` 未加入拦截器白名单 → 登录页死循环频闪 | 加入白名单，由 Controller 自行判断登录态 |
| 5 | 路由守卫对 `/login` 也调 `checkAuth()` → 不必要的 API 调用 | 仅对 `requiresAuth` 页面调用 |
| 6 | App.vue 用 `$route.path === '/login'` 判断布局 → 初始渲染闪主界面 | 改用 `!authStore.isLoggedIn` 判断 |
| 7 | `checkAuth()` 在 `data` 为 `null` 时仍设 `isLoggedIn = true` → 未登录显示主界面 | 加 `if (data)` 判断 |
| 8 | 手动 INSERT 用户密码为明文 → bcrypt 校验失败无法登录 | 必须由 `DataInitializer` 创建用户 |
| 9 | 登录框不居中 | `.login-page` 添加 `width: 100%` |

## 9. 依赖

无新增依赖。bcrypt 使用已有 Hutool，不引入 Spring Security。
