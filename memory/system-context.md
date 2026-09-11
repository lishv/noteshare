# Note Sharing Platform - Context & Architecture Reference

## 1. Project Tech Stack & Layout
- **Backend (`project/note-backend`)**:
  - Spring Boot 3.2.0, Java 17, MyBatis-Plus 3.5.5, MySQL 8 (`note_sharing`), Redis (db 9, Lettuce pool), Spring Security (stateless + BCrypt), JJWT 0.12.3, Aliyun OSS, SpringDoc OpenAPI.
  - Context path: `/api`, Port: 8080 (Single instance), 8081-8083 (Cluster profile).
- **User Frontend (`project/note-frontend/user`)**:
  - Vue 3, Vite (port 5173, proxy `/api` -> 8080), TypeScript, Pinia, Vue Router, Element Plus, Axios.
- **Admin Frontend (`project/note-frontend/admin`)**:
  - Vue 3, Vite (port 5174, proxy `/api` -> 8080), TypeScript, Pinia, Vue Router, Element Plus, ECharts, Axios.

## 2. Core Business Rules & Flows
- **Auth & JWT**:
  - User and Admin JWTs are separated (`jwt.secret` vs `admin.jwt.secret`), verified via `AuthInterceptor` and `AdminAuthInterceptor`.
  - Login brute force protection: 5 failures within 10 mins locks account/IP (`t_login_fail_record`, `t_admin_login_fail_record`).
  - Email verification codes cached in Redis with 60s cooldown, 5m expiration, 10/day rate limit.
- **Notes & Main Links**:
  - Note creation strictly requires an existing `MainLink` (`t_main_link`).
  - Sensitive words auto-checked on title and URLs (`t_sensitive_word` + Redis cache).
  - Soft delete support (`is_deleted`, `deleted_at`) with MyBatis-Plus logic delete.
  - Pinned notes (`is_pinned`), tagging (`t_tag`, `t_note_tag`), favorites (`t_note_favorite`), view count increments.
- **Admin Management & Audit**:
  - Role 1 (Super Admin), Role 2 (Normal Admin). Super Admin cannot be deleted/disabled; last active Super Admin cannot be disabled.
  - User management: blacklist in Redis (`user:blacklist:<username>`), toggle status, reset password, unlock, cascading deletes.
  - Sensitive words: batch import/delete, toggle status, Redis cache invalidation/refresh.
  - Audit logging: `t_operation_log` records admin operations (LOGIN, USER_MANAGE, NOTE_MANAGE, LINK_MANAGE, WORD_MANAGE, ADMIN_MANAGE), auto-cleaned by `LogCleanupTask`.
  - Statistics: Dashboard metrics, growth trend, category distribution, user activity ranking.
