# 学生笔记共享平台 - 前端

基于 Vue 3 + TypeScript + Element Plus 的学生笔记共享平台前端项目。

## 技术栈

- **框架**: Vue 3.4 + TypeScript
- **UI 组件库**: Element Plus 2.5
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **构建工具**: Vite 5
#文档访问地址
##地址	说明
##http://localhost:8080/api/swagger-ui/index.html	Swagger UI 页面
##http://localhost:8080/api/swagger-ui.html	Swagger UI 入口
##http://localhost:8080/api/api-docs	OpenAPI JSON 文档
##http://localhost:8080/api/api-docs/public	Public API 分组
##http://localhost:8080/api/api-docs/private	Private API 分组
##可以直接在浏览器中访问 Swagger UI，也可以将 JSON 文档复制到 Apifox 导入使用。
## 项目结构

```
note-frontend/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口定义
│   │   ├── user.ts     # 用户相关接口
│   │   └── note.ts     # 笔记相关接口
│   ├── assets/         # 静态资源
│   ├── components/     # 公共组件
│   │   └── PublishDialog.vue  # 发布/编辑笔记弹窗
│   ├── router/         # 路由配置
│   │   └── index.ts
│   ├── store/          # 状态管理
│   │   └── user.ts     # 用户状态
│   ├── types/          # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/          # 工具函数
│   │   ├── request.ts  # Axios 封装
│   │   └── helpers.ts  # 辅助函数
│   ├── views/          # 页面组件
│   │   ├── Home.vue    # 主页（左右分栏）
│   │   ├── Login.vue   # 登录页
│   │   ├── Register.vue# 注册页
│   │   └── Settings.vue# 个人设置页
│   ├── App.vue         # 根组件
│   ├── env.d.ts        # 类型声明
│   └── main.ts         # 入口文件
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 功能特性

### 已实现功能

1. **用户认证**
   - 用户注册（用户名唯一性校验、邮箱校验）
   - 用户登录（支持记住登录状态）
   - 用户登出
   - 密码修改
   - 密码重置（发送邮件）

2. **笔记管理**
   - 发布新笔记（支持总链接和单篇笔记）
   - 编辑笔记
   - 删除笔记（二次确认）
   - 笔记列表展示

3. **浏览与搜索**
   - 左侧活跃用户列表（按最后活跃时间排序）
   - 右侧笔记详情展示
   - 全局搜索（支持姓名和笔记名称模糊匹配）
   - 笔记浏览量统计

4. **安全特性**
   - URL 协议校验（仅允许 http/https/ftp）
   - 敏感词检测
   - XSS 防护
   - Token 自动管理

5. **响应式设计**
   - 移动端抽屉式导航
   - 自适应布局

## 开发指南

### 环境要求

- Node.js >= 18
- npm >= 9

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 类型检查

```bash
npm run type-check
```

## API 配置

开发环境下，API 请求会代理到后端服务（默认 http://localhost:8080）。

在 `vite.config.ts` 中修改代理配置：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

## 核心接口

详见 `src/api/` 目录下的接口定义文件。

### 用户接口 (user.ts)
- `register()` - 用户注册
- `login()` - 用户登录
- `logout()` - 用户登出
- `getCurrentUser()` - 获取当前用户信息
- `changePassword()` - 修改密码
- `sendResetPassword()` - 发送重置密码邮件

### 笔记接口 (note.ts)
- `getActiveUsers()` - 获取活跃用户列表
- `setMainLink()` - 设置总链接
- `getMainLink()` - 获取用户总链接
- `deleteMainLink()` - 删除总链接
- `createNote()` - 创建笔记
- `getUserNotes()` - 获取用户笔记列表
- `updateNote()` - 更新笔记
- `deleteNote()` - 删除笔记
- `globalSearch()` - 全局搜索

## 注意事项

1. 所有写操作需要登录后才能进行
2. 用户只能编辑/删除自己的笔记
3. 删除操作需要二次确认
4. 敏感内容会被拦截
5. 移动端使用抽屉式导航

## 待开发功能

- 浏览记录/历史记录
- 笔记分类/标签
- 收藏功能
- 用户间关注/粉丝系统
- 笔记评论/点赞
