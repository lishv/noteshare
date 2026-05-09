# 学生笔记共享平台 - 后端服务

#用户端 #账号: zhangsan #密码: password123 #管理端超级管理员数据

# 默认账号: admin

# 默认密码: admin123

#直接双击项目根目录下的 start.bat 文件，它会自动执行上述步骤。

<!-- 启动应用后，访问以下地址查看文档：


端点	URL	状态
Swagger UI	http://localhost:8080/api/swagger-ui/index.html
Swagger UI (快捷)	http://localhost:8080/api/swagger-ui.html	重定向到上面
OpenAPI JSON	http://localhost:8080/api/openapi.json	即可获得完整的 API 文档
可以将返回的 JSON 内容复制到 Apifox 或其他 API 工具中使用 -->

## 项目结构

```
note-backend/
├── src/main/java/com/note/
│   ├── NoteApplication.java          # 应用入口
│   ├── config/                        # 配置类
│   │   ├── WebConfig.java            # Web MVC 配置（拦截器）
│   │   └── MybatisPlusConfig.java    # MyBatis-Plus 分页配置
│   ├── controller/                    # 控制器层
│   │   ├── AuthController.java       # 认证接口
│   │   ├── UserController.java       # 用户接口
│   │   ├── MainLinkController.java   # 总链接接口
│   │   ├── NoteController.java       # 笔记接口
│   │   └── SearchController.java     # 搜索接口
│   ├── service/                       # 业务逻辑层
│   │   ├── AuthService.java          # 认证服务
│   │   ├── UserService.java          # 用户服务
│   │   ├── MainLinkService.java      # 总链接服务
│   │   ├── NoteService.java          # 笔记服务
│   │   ├── SearchService.java        # 搜索服务
│   │   └── SensitiveWordService.java # 敏感词服务
│   ├── mapper/                        # MyBatis Mapper 接口
│   │   ├── UserMapper.java
│   │   ├── MainLinkMapper.java
│   │   ├── NoteItemMapper.java
│   │   ├── LoginFailRecordMapper.java
│   │   └── SensitiveWordMapper.java
│   ├── entity/                        # 实体类
│   │   ├── User.java
│   │   ├── MainLink.java
│   │   ├── NoteItem.java
│   │   ├── LoginFailRecord.java
│   │   └── SensitiveWord.java
│   ├── dto/                           # 数据传输对象
│   │   ├── RegisterDTO.java
│   │   ├── LoginDTO.java
│   │   ├── ChangePasswordDTO.java
│   │   ├── NoteDTO.java
│   │   └── MainLinkDTO.java
│   ├── vo/                            # 视图对象
│   │   ├── UserVO.java
│   │   ├── NoteVO.java
│   │   ├── MainLinkVO.java
│   │   ├── ActiveUserVO.java
│   │   ├── LoginVO.java
│   │   ├── PageResult.java
│   │   └── SearchVO.java
│   ├── enums/                         # 枚举类
│   │   └── ErrorCode.java            # 错误码枚举
│   ├── exception/                     # 异常处理
│   │   ├── BusinessException.java    # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   ├── interceptor/                   # 拦截器
│   │   └── AuthInterceptor.java      # JWT 认证拦截器
│   ├── util/                          # 工具类
│   │   └── JwtUtil.java              # JWT 工具类
│   └── service/impl/                  # 服务实现
└── src/main/resources/
    ├── application.yml               # 应用配置
    └── init.sql                      # 数据库初始化脚本
```

## 技术栈

- **框架**: Spring Boot 3.2.x
- **安全**: Spring Security (JWT 认证)
- **ORM**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.x
- **验证**: Jakarta Validation
- **其他**: Lombok, Hutool, JJWT

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 7.x

### 2. 数据库初始化

```bash
mysql -u root -p < src/main/resources/init.sql
```

### 3. 修改配置文件

编辑 `src/main/resources/application.yml`，配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/note_sharing?...
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package
java -jar target/note-sharing-platform-1.0.0.jar
```

## API 接口说明

### 认证相关

| 方法 | 路径               | 说明     |
| ---- | ------------------ | -------- |
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login    | 用户登录 |

### 用户相关

| 方法 | 路径                | 说明             |
| ---- | ------------------- | ---------------- |
| GET  | /api/users/current  | 获取当前用户信息 |
| PUT  | /api/users/password | 修改密码         |
| GET  | /api/users/active   | 获取活跃用户列表 |

### 总链接相关

| 方法   | 路径                    | 说明            |
| ------ | ----------------------- | --------------- |
| POST   | /api/main-link          | 设置/更新总链接 |
| GET    | /api/main-link/{userId} | 获取用户总链接  |
| DELETE | /api/main-link          | 删除我的总链接  |

### 笔记相关

| 方法   | 路径                     | 说明             |
| ------ | ------------------------ | ---------------- |
| POST   | /api/notes               | 创建笔记         |
| GET    | /api/notes/user/{userId} | 获取用户笔记列表 |
| GET    | /api/notes/{noteId}      | 获取单个笔记详情 |
| PUT    | /api/notes/{noteId}      | 更新笔记         |
| DELETE | /api/notes/{noteId}      | 删除笔记         |
| POST   | /api/notes/{noteId}/view | 增加浏览量       |

### 搜索相关

| 方法 | 路径                  | 说明     |
| ---- | --------------------- | -------- |
| GET  | /api/search?keyword=X | 全局搜索 |

## 安全特性

- 登录失败 5 次锁定账号 10 分钟
- 密码使用 BCrypt 加密存储
- JWT Token 有效期 7 天
- 敏感词过滤（DFA 算法）

## 数据字典

### 用户表 (t_user)

- status: 1-正常，0-禁用，2-锁定

### 敏感词表 (t_sensitive_word)

- type: 1-政治，2-色情，3-暴力，4-广告
- status: 1-启用，0-禁用

### 笔记表 (t_note_item)

- is_deleted: 1-已删除，0-未删除（逻辑删除）
