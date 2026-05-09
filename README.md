# 学生笔记共享平台

一个基于 Spring Boot + Vue 3 的学生笔记共享平台，包含用户端和管理端。

## 访问地址

| 服务         | 地址                                      | 说明                          |
| ------------ | ----------------------------------------- | ----------------------------- |
| 用户端前端   | http://localhost:5173                     | Vite 开发模式 (Vue 3 + Element Plus) |
| 用户端前端   | http://localhost:80                       | nginx 生产模式                |
| 管理端前端   | http://localhost:5174                     | Vite 开发模式 (Vue 3 + Element Plus + ECharts) |
| 管理端前端   | http://localhost:81                       | nginx 生产模式                |
| 后端 API     | http://localhost:8080/api                 | Spring Boot 3.2              |
| Swagger 文档 | http://localhost:8080/api/swagger-ui.html | 接口文档             |
| OpenAPI JSON | http://localhost:8080/api/openapi.json    | 可导入 Apifox        |

## 默认账号

| 角色       | 用户名   | 密码     | 说明                 |
| ---------- | -------- | -------- | -------------------- |
| 超级管理员 | admin    | admin123 | 登录后请立即修改密码 |
| 普通用户   | 自行注册 | 自定义   | 需邮箱验证码注册     |

## 技术栈

### 后端

- Java 17 + Spring Boot 3.2
- MyBatis-Plus 3.5（ORM）
- Spring Security + JWT（认证授权）
- Redis（缓存/登录锁定/黑名单）
- MySQL 8.0（数据存储）
- Springdoc OpenAPI（接口文档）
- 阿里云 OSS（文件上传）

### 用户端前端

- Vue 3.4 + TypeScript
- Element Plus 2.5
- Pinia（状态管理）
- Vue Router 4
- Axios（HTTP 请求）
- Vite 5（构建工具）

### 管理端前端

- Vue 3.4 + TypeScript
- Element Plus 2.5
- ECharts 5.4（数据可视化）
- Pinia（状态管理）
- Vue Router 4
- Axios（HTTP 请求）
- Vite 5（构建工具）

## 项目结构

```
project/
├── note-backend/                 # 后端服务
│   └── src/main/java/com/note/
│       ├── controller/
│       │   ├── user/            # 用户端 Controller（6个）
│       │   └── admin/           # 管理端 Controller（9个）
│       ├── service/              # 业务逻辑层
│       ├── mapper/               # 数据访问层
│       ├── entity/               # 实体类
│       ├── dto/                  # 请求参数
│       ├── vo/                   # 响应数据
│       ├── config/               # 配置类
│       ├── interceptor/          # 拦截器
│       ├── enums/                # 错误码枚举
│       ├── exception/            # 异常处理
│       ├── util/                 # 工具类
│       └── scheduled/            # 定时任务
│   └── src/main/resources/
│       ├── mapper/               # MyBatis XML
│       ├── application.yml       # 配置文件
│       ├── init.sql              # 用户端数据库初始化
│       └── init_admin.sql        # 管理端数据库初始化
│
├── note-frontend/
│   ├── user/                     # 用户端前端（已完成）
│   └── admin/                    # 管理端前端（已完成）
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis

### 1. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS note_sharing DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 初始化用户端表
mysql -u root -p note_sharing < project/note-backend/src/main/resources/init.sql

# 初始化管理端表
mysql -u root -p note_sharing < project/note-backend/src/main/resources/init_admin.sql
```

### 2. 配置后端

编辑 `project/note-backend/src/main/resources/application.yml`，修改数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/note_sharing?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
```

### 3. 启动后端

#### 方式一：Docker（推荐）

```bash
cd project/note-backend
docker-compose up -d
```

自动启动 MySQL + Redis + 后端服务，数据库使用 `init.sql` + `init_admin.sql` + `init_with_test_data.sql` 自动初始化。

#### 方式二：手动启动

```bash
cd project/note-backend
mvn spring-boot:run
```

### 4. 启动用户端前端

```bash
cd project/note-frontend/user
npm install
npm run dev
```

### 5. 启动管理端前端

```bash
cd project/note-frontend/admin
npm install
npm run dev
```

## 管理端 API 概览

管理端接口前缀 `/api/admin/`，共 45 个接口，分为 9 个模块：

| 模块       | 路径               | 端点数 | 说明                                           |
| ---------- | ------------------ | ------ | ---------------------------------------------- |
| 认证管理   | `/admin/auth`      | 4      | 登录、登出、个人信息、改密                     |
| 仪表盘     | `/admin/dashboard` | 5      | 概览统计、用户/笔记趋势、热门笔记              |
| 用户管理   | `/admin/users`     | 6      | 列表、详情、启禁用、解锁、重置密码             |
| 笔记管理   | `/admin/notes`     | 6      | 列表、详情、逻辑删除/恢复、批量操作            |
| 链接管理   | `/admin/links`     | 3      | 列表、编辑、删除                               |
| 敏感词管理 | `/admin/words`     | 8      | CRUD、批量导入/删除/启禁用                     |
| 数据统计   | `/admin/stats`     | 7      | 注册/活跃/分布/发布/浏览排名/链接类型/平台趋势 |
| 操作日志   | `/admin/logs`      | 1      | 分页查询（支持类型和时间筛选）                 |
| 管理员管理 | `/admin/admins`    | 5      | 超管专用：列表、创建、编辑、启禁用、重置密码   |

## 生产部署

项目提供集群部署方案，配置文件位于 `project/deploy/`：

| 文件 | 说明 |
|------|------|
| `nginx-lb.conf` | Nginx 负载均衡配置（用户端 :80，管理端 :81，后端集群 8081/8082/8083） |
| `start-cluster.bat` | 启动 3 个后端实例 |
| `stop-cluster.bat` | 停止集群 |

```bash
# 1. 构建后端
cd project/note-backend
mvn package -DskipTests

# 2. 启动后端集群
cd ../deploy
start-cluster.bat

# 3. 配置 Nginx 使用 nginx-lb.conf
```

## 开发文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 用户端 PRD | `prd.md` | 用户端产品需求文档 |
| 管理端 PRD | `admin-prd.md` | 管理端产品需求文档 |
| 用户端数据库设计 | `用户端数据库与接口设计计划.md` | 用户端表结构和接口设计 |
| 管理端数据库设计 | `管理端数据库与接口设计计划.md` | 管理端表结构和接口设计 |
| 管理端接口详情 | `管理端接口数据库.md` | 45 个接口完整 Request/Response JSON 示例 |
| API 文档（Apifox） | `combined-api-docs.json` | OpenAPI 3.0 格式（含用户端 + 管理端，可直接导入 Apifox）|
