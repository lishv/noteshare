# 学生笔记共享平台 - 后端服务

## 账号信息

| 角色 | 用户名 | 默认密码 |
|------|--------|---------|
| 用户端 | 自行注册 | - |
| 管理端超管 | admin | admin123（首次登录需修改） |

## 项目结构

```
src/main/java/com/note/
├── NoteApplication.java              # 应用入口
├── config/                            # 配置类
│   ├── SecurityConfig.java           # Spring Security
│   ├── WebConfig.java                # 拦截器注册
│   ├── MybatisPlusConfig.java        # 分页插件
│   ├── RedisConfig.java              # Redis 配置
│   └── OpenApiConfig.java            # Swagger 配置
├── controller/
│   ├── user/                         # 用户端接口（6个）
│   └── admin/                        # 管理端接口（9个，45个端点）
├── service/                           # 业务逻辑层（17个）
├── mapper/                            # MyBatis-Plus Mapper
├── entity/                            # 数据实体
├── dto/                               # 请求参数
├── vo/                                # 响应数据
├── interceptor/
│   ├── AuthInterceptor.java          # 用户端 JWT 拦截器
│   └── AdminAuthInterceptor.java     # 管理端 JWT 拦截器
├── util/
│   ├── JwtUtil.java                  # 用户端 JWT
│   └── AdminJwtUtil.java             # 管理端 JWT（独立密钥）
├── enums/ErrorCode.java              # 错误码（1xxx-6xxx）
├── exception/                        # 异常处理
└── scheduled/
    └── LogCleanupTask.java           # 操作日志定时清理

src/main/resources/
├── application.yml                    # 应用配置
├── init.sql                          # 用户端建表
├── init_admin.sql                    # 管理端建表
└── mapper/*.xml                      # Mapper XML
```

## 技术栈

- Spring Boot 3.2 + Java 17
- Spring Security + JWT（用户端/管理端双密钥隔离）
- MyBatis-Plus 3.5 + MySQL 8.0
- Redis 7（缓存 + 登录锁定 + 黑名单）
- SpringDoc OpenAPI（Swagger 文档）
- Lombok + Hutool + BCrypt

## 快速开始

### 本地开发

**1. 环境要求**

- JDK 17+
- MySQL 8.0+
- Redis 7.x
- Maven 3.9+

**2. 初始化数据库**

```bash
mysql -u root -p < src/main/resources/init.sql
mysql -u root -p < src/main/resources/init_admin.sql
```

**3. 修改配置**

编辑 `src/main/resources/application.yml`，修改数据库和 Redis 连接信息。

**4. 启动**

```bash
mvn spring-boot:run
# 或
mvn clean package -DskipTests
java -jar target/note-sharing-platform-1.0.0.jar
```

### Docker 部署（Linux 虚拟机）

**1. 安装 Docker**

```bash
# Ubuntu
sudo apt update && sudo apt install -y docker.io docker-compose
sudo systemctl enable docker && sudo systemctl start docker

# CentOS
sudo yum install -y docker docker-compose-plugin
sudo systemctl enable docker && sudo systemctl start docker
```

**2. 上传项目**

```bash
# 从 Gitee 克隆
cd /opt
git clone https://gitee.com/liShuwen123/noteshare.git
cd noteshare/project/note-backend
```

**3. 配置环境变量（可选）**

```bash
# 自定义密码，不设置则使用默认值
export MYSQL_ROOT_PASSWORD=YourStrongPassword
export REDIS_PASSWORD=YourStrongPassword
```

**4. 一键启动**

```bash
docker compose up -d
```

Docker 会自动：
- 拉取 MySQL 8.0 和 Redis 7 镜像
- 执行 `init.sql` → `init_admin.sql` → `init_with_test_data.sql` 初始化数据库
- 构建后端 JAR 并启动

**5. 常用命令**

```bash
docker compose ps                    # 查看容器状态
docker compose logs -f backend       # 查看后端日志
docker compose restart backend       # 重启后端
docker compose down                  # 停止所有服务
docker compose down -v               # 停止并删除数据卷
```

**6. 验证部署**

```bash
curl http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 用户端前端 | http://localhost |
| 管理端前端 | http://localhost:81 |
| 后端 API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/openapi.json |

## API 总览

### 用户端 `/api/user/`

| 模块 | 端点数 | 路径前缀 |
|------|--------|---------|
| 认证管理 | 4 | `/user/auth` |
| 用户管理 | 3 | `/user/users` |
| 笔记管理 | 5 | `/user/notes` |
| 总链接管理 | 3 | `/user/main-link` |
| 搜索 | 1 | `/user/search` |
| 文件上传 | 1 | `/user/upload` |

### 管理端 `/api/admin/`

| 模块 | 端点数 | 路径前缀 |
|------|--------|---------|
| 认证管理 | 4 | `/admin/auth` |
| 仪表盘 | 5 | `/admin/dashboard` |
| 用户管理 | 6 | `/admin/users` |
| 笔记管理 | 6 | `/admin/notes` |
| 链接管理 | 3 | `/admin/links` |
| 敏感词管理 | 8 | `/admin/words` |
| 数据统计 | 7 | `/admin/stats` |
| 操作日志 | 1 | `/admin/logs` |
| 管理员管理 | 5 | `/admin/admins`（仅超管） |

## 安全特性

- 登录失败 5 次锁定账号 10 分钟（用户端/管理端独立）
- 密码 BCrypt 加密存储
- 用户端 JWT 7 天有效期，管理端 JWT 24 小时有效期
- 用户端/管理端 JWT 密钥隔离
- 管理员禁用用户时自动加入 Redis 黑名单
- 敏感词 DFA 算法过滤
- 操作日志审计（90 天自动清理）
- 超级管理员保护（不可禁用最后一个超管）

## 数据表

| 表名 | 说明 |
|------|------|
| t_user | 用户表（status: 0=禁用, 1=正常, 2=锁定） |
| t_main_link | 总链接表（一对一） |
| t_note_item | 笔记表（is_deleted: 逻辑删除） |
| t_login_fail_record | 用户登录失败记录 |
| t_sensitive_word | 敏感词表（type: 1=政治, 2=色情, 3=暴力, 4=广告） |
| t_admin | 管理员表（role: 1=超管, 2=普通） |
| t_admin_login_fail_record | 管理员登录失败记录 |
| t_operation_log | 操作日志表 |
