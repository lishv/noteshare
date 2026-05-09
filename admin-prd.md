# 学生笔记共享平台 — 管理端 PRD

## 一、产品概述
管理端是学生笔记共享平台的运营后台，供平台管理员使用。核心能力包括：管理员登录认证、用户管理、笔记内容管理、总链接管理、敏感词管理、数据统计分析（ECharts图表）、管理员账号管理、操作日志审计。管理端与用户端共用同一套数据库，后端独立一套 `/api/admin` 接口，前端独立部署为单独的 Vue 应用。

## 二、角色与权限
| 角色 | 权限范围 |
|------|---------|
| 超级管理员 (Super Admin) | 拥有所有管理权限，可管理其他管理员的账号（增删改查、启用禁用、重置密码） |
| 普通管理员 (Admin) | 拥有除"管理员账号管理"以外的所有权限：用户管理、笔记管理、总链接管理、敏感词管理、数据统计查看、操作日志查看 |

## 三、管理员账号体系
### 1. 管理员登录
- 使用用户名 + 密码登录，登录成功后返回 JWT Token
- 登录失败5次后锁定账号10分钟（逻辑复用用户端，数据独立存储）
- 管理端 Token 与用户端 Token 隔离（使用不同的JWT签名密钥或Token前缀区分）
- 管理员表 `t_admin` 与用户表 `t_user` 完全分离，独立管理

### 2. 管理员个人设置
- 支持通过邮箱验证码修改密码
- 支持上传/修改管理员头像（复用已有的阿里云 OSS 上传能力）
- 支持修改邮箱

### 3. 超级管理员初始化
- 通过 SQL 脚本在 `init.sql` 中 INSERT 一条默认超级管理员记录
- 默认账号：`admin` / 默认密码：`admin123`（首次登录后强制修改密码）

## 四、核心界面与交互
管理端采用经典的**顶部导航 + 左侧菜单 + 右侧内容区**后台布局。

### 1. 顶部导航栏
- 左侧：平台 Logo + "笔记共享平台管理后台" 名称
- 右侧：管理员头像 + 用户名 + "修改密码" + "退出登录"

### 2. 左侧菜单结构
```
├── 首页仪表盘 (Dashboard)
├── 用户管理
├── 笔记管理
├── 总链接管理
├── 敏感词管理
├── 数据统计
├── 操作日志
└── 管理员管理 (仅超级管理员可见)
```

### 3. 页面详情

#### 3.1 首页仪表盘 (Dashboard)
- **数据概览卡片**（顶部一排四个卡片）：
  - 总用户数（附今日新增数）
  - 总笔记数（附今日新增数）
  - 总浏览量（附今日浏览量）
  - 活跃用户数（最近7天有活跃的用户数）
- **图表区域**：
  - 近7天/30天用户注册趋势折线图
  - 近7天/30天笔记发布趋势折线图
  - 笔记浏览量 Top 10 排行柱状图
  - 用户活跃度分布饼图（活跃/7天未活跃/30天未活跃）

#### 3.2 用户管理
- **列表展示**：用户名、邮箱、头像、状态、笔记数量、最后活跃时间、注册时间
- **搜索/筛选**：
  - 支持按用户名/邮箱模糊搜索
  - 按状态筛选（正常/禁用/锁定）
- **操作**：
  - 查看详情：查看该用户的所有笔记和总链接
  - 禁用/启用：修改用户状态（status: 1正常 ↔ 0禁用），禁用时弹出二次确认
  - 解锁：将锁定状态(status=2)的用户恢复为正常(status=1)
  - 重置密码：将用户密码重置为默认密码，并通过邮件通知用户
- **批量操作**：勾选多个用户后批量禁用/启用
- **分页**：每页20条

#### 3.3 笔记管理
- **列表展示**：笔记名称、所属用户、链接/位置、浏览量、发布时间、更新时间、状态(正常/已删除)
- **搜索/筛选**：
  - 支持按标题/用户名模糊搜索
  - 按状态筛选（正常/已逻辑删除）
- **操作**：
  - 查看详情
  - 强制删除：管理员可删除任何用户的笔记（逻辑删除）
  - 恢复笔记：将逻辑删除的笔记恢复为正常状态
- **批量操作**：勾选多条笔记后批量删除/恢复
- **分页**：每页20条

#### 3.4 总链接管理
- **列表展示**：所属用户、总链接URL、浏览量、更新时间
- **搜索**：支持按用户名/链接内容模糊搜索
- **操作**：
  - 强制删除：删除指定用户的总链接
  - 编辑：修改指定用户的总链接内容
- **分页**：每页20条

#### 3.5 敏感词管理
- **列表展示**：敏感词、分类(政治/色情/暴力/广告)、状态(启用/禁用)、创建时间
- **搜索/筛选**：按关键词搜索，按分类/状态筛选
- **操作**：
  - 新增：输入敏感词 + 选择分类
  - 编辑：修改敏感词或分类
  - 启用/禁用：切换敏感词的生效状态
  - 删除：删除敏感词
- **批量操作**：
  - 批量导入：文本框批量粘贴，一行一个敏感词 + 选择分类
  - 批量删除：勾选多个敏感词后批量删除
  - 批量启用/禁用
- **分页**：每页20条

#### 3.6 数据统计
- **用户维度**：
  - 用户注册趋势图（折线图，可选7天/30天/90天）
  - 用户活跃度统计（柱状图，按日/周/月）
  - 用户笔记数量分布图（柱状图，0篇/1-5篇/6-10篇/10篇以上）
- **笔记维度**：
  - 笔记发布趋势图（折线图，可选7天/30天/90天）
  - 笔记浏览量排行 Top 20（水平柱状图）
  - 笔记链接类型占比（饼图：URL链接 vs 相对位置文本）
- **平台维度**：
  - 平台总数据量趋势（面积图，叠加用户数+笔记数）
- **图表库**：使用 ECharts

#### 3.7 操作日志
- **列表展示**：操作人、操作类型、操作描述、操作对象、IP地址、操作时间
- **操作类型分类**：登录、用户管理、笔记管理、总链接管理、敏感词管理、管理员管理
- **搜索/筛选**：按操作人搜索，按操作类型筛选，按时间范围筛选
- **分页**：每页20条
- **保留策略**：日志保留90天，超期自动清理

#### 3.8 管理员管理 (仅超级管理员可见)
- **列表展示**：用户名、邮箱、头像、角色、最后登录时间、状态、创建时间
- **操作**：
  - 新增管理员：填写用户名、密码、邮箱、角色（超级管理员/普通管理员）
  - 编辑：修改角色、邮箱
  - 启用/禁用管理员账号
  - 重置密码：将管理员密码重置为默认密码，通过邮件通知
- **分页**：每页20条

## 五、核心业务流程

### 1. 管理员登录流程
输入用户名密码 → 后端校验 → 检查账号锁定状态 → 检查账号启用状态 → 生成管理端JWT → 记录登录日志 → 返回Token → 前端存储并跳转仪表盘

### 2. 禁用用户流程
管理员点击"禁用" → 二次确认弹窗 → 后端将 t_user.status 改为 0 → 将该用户已发放的JWT加入Redis黑名单 → 该用户立即失效 → 记录操作日志

### 3. JWT黑名单机制
- 管理员禁用用户时，将该用户的用户名写入Redis黑名单（key: `user:blacklist:{username}`，TTL与用户端JWT有效期一致）
- 用户端AuthInterceptor在每次请求时，除了校验用户状态外，额外检查该用户是否在黑名单中
- 黑名单key过期后自动清理，无需手动维护

### 4. 敏感词新增流程
管理员输入敏感词 → 选择分类 → 后端校验是否重复 → 写入 t_sensitive_word 表 → 同步更新 Redis 缓存 → 记录操作日志

### 5. 修改密码流程
管理员点击"修改密码" → 输入当前密码 + 新密码 → 后端校验当前密码 → 更新密码哈希 → 记录操作日志 → 返回成功

### 6. 操作日志记录规则
以下操作自动记录日志：
- 管理员登录/登出
- 禁用/启用/解锁用户、重置用户密码
- 删除/恢复笔记、编辑总链接
- 新增/编辑/删除/启用/禁用敏感词、批量导入敏感词
- 新增/编辑/禁用管理员、重置管理员密码

## 六、数据模型设计

### 1. 新增表：t_admin (管理员表)
```sql
CREATE TABLE `t_admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色: 1-超级管理员, 2-普通管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';
```

### 2. 新增表：t_admin_login_fail_record (管理员登录失败记录表)
```sql
CREATE TABLE `t_admin_login_fail_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
  `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `fail_count` INT NOT NULL DEFAULT 1 COMMENT '失败次数',
  `lock_until` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_ip` (`username`, `ip_address`),
  KEY `idx_lock_until` (`lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员登录失败记录表';
```

### 3. 新增表：t_operation_log (操作日志表)
```sql
CREATE TABLE `t_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` BIGINT NOT NULL COMMENT '操作管理员ID',
  `admin_username` VARCHAR(50) NOT NULL COMMENT '操作管理员用户名',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型: LOGIN/USER_MANAGE/NOTE_MANAGE/LINK_MANAGE/WORD_MANAGE/ADMIN_MANAGE',
  `description` VARCHAR(500) NOT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型: USER/NOTE/MAIN_LINK/SENSITIVE_WORD/ADMIN',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作对象ID',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

### 4. 超级管理员初始化数据
```sql
-- 默认超级管理员账号，首次登录后请立即修改密码
INSERT INTO `t_admin` (`username`, `password`, `email`, `role`, `status`)
VALUES ('admin', '$2a$10$默认BCrypt哈希值', 'admin@example.com', 1, 1);
-- 默认密码: admin123
```

## 七、技术方案要点

### 1. 后端接口前缀
管理端所有接口统一使用 `/api/admin` 前缀，与用户端 `/api/user` 隔离。在后端 `controller/admin/` 目录下新建管理端Controller。

### 2. 前端项目结构
管理端前端独立为 `note-frontend/admin/` 目录，与用户端 `note-frontend/user/` 平级，独立构建部署。
- 技术栈：Vue 3 + TypeScript + Element Plus + ECharts + Pinia + Axios
- Axios baseURL: `/api/admin`

### 3. 认证与安全
- 管理端JWT与用户端JWT使用不同的签名密钥（jwt.admin.secret）
- 所有管理端接口需携带管理端Token，通过独立的AdminAuthInterceptor校验
- Token有效期建议24小时（比用户端的7天更短，提升安全性）
- 管理员禁用用户时，通过Redis黑名单使该用户的用户端Token立即失效

### 4. 数据统计接口设计思路
- 统计类接口通过SQL聚合查询实现（COUNT、GROUP BY DATE等）
- 热门排行类数据通过 ORDER BY view_count DESC LIMIT N 查询
- 统计数据可考虑Redis缓存（TTL 5分钟），减轻数据库压力
