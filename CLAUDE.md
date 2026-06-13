# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**项目规则**: 请同时遵守 [.claude/rules/shuike-manager.md](.claude/rules/shuike-manager.md) 中的全部规则。

---

## Project Overview

### 产品定位

**水课管理系统 (ShuiKe Manager)** — 高校教学材料智能化分析与审核平台。

### 核心业务流程

```
教师提交教学材料（授课计划/教案/课件/考核方案）
    → AI 大模型自动四维评审（内容完整性/课程标准匹配度/格式规范性/创新性）
    → 学院主任审核（通过/驳回）
    → 教务处终审（通过/驳回）
    → 教师查看审核反馈 + 通知中心实时推送
```

### 系统架构

```
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│ Vue 3 前端   │────▶│ Nginx 反向代理 │────▶│ Spring Boot API │
│ Element Plus │     │ (静态资源+代理) │     │ (REST JSON)    │
│ ECharts 图表 │     └──────────────┘     └───┬───┬───┬────┘
└─────────────┘                               │   │   │
                                   ┌─────────▼┐ ┌▼──┐ ┌▼───┐
                                   │ MySQL 8  │ │Redis│ │MinIO│
                                   │ 业务数据  │ │缓存 │ │文件 │
                                   └──────────┘ └────┘ └─────┘
                                              │
                                   ┌──────────▼──────────┐
                                   │ 火山引擎大模型 API    │
                                   │ (豆包/DeepSeek)      │
                                   └─────────────────────┘
```

### 功能模块

| 模块 | 说明 |
|------|------|
| **材料提交与评审** | 教师上传教学材料 → AI 自动评分+生成优化建议 → 主任审核 → 教务处终审 |
| **AI 智能评审** | 火山引擎 LLM 四维并发评审（内容完整性30% + 课标匹配度30% + 格式规范性20% + 创新性20%） |
| **Prompt 模板管理** | 4种材料类型×4个评审维度=16个专业模板，教务处可在线编辑，自动版本管理 |
| **多角色工作台** | 教师/学院主任/教务处/院长 4 角色专属 Dashboard，含 ECharts 统计图表 |
| **数据隔离** | 主任仅见本院材料，教师仅见个人材料，院长仅见本学院人培方案 |
| **对齐分析** | 院长发起产业需求对齐分析，LLM 生成 8 章节论文式报告（产业背景/人才需求/课程体系/学习成果/就业前景/差距分析/改进建议/综合评分） |
| **通知中心** | 全流程通知（材料提交/AI完成/审核结果/终审提醒），铃铛角标 30s 自动刷新 |
| **材料归档** | 教务处按类型/课程/教师搜索，在线预览 Word/PDF 文件，批量下载 |
| **用户管理** | CSV 批量导入，学院-角色关联，密码默认规则 |
| **文件存储** | MinIO 按材料类型分目录存储，删除材料同步清理 MinIO 文件和 AI 评审记录 |

### 技术架构

- **后端**: Spring Boot 2.7.x + Java 8 + MyBatis-Plus 3.5 + MySQL 5.6
- **前端**: Vue 3.4 + TypeScript + Element Plus 2.7 + Pinia 2.x + ECharts 5.x
- **认证**: JWT (jjwt 0.11.5) + Spring Security + RBAC（4角色细粒度权限）
- **AI 集成**: 火山引擎方舟 API，RestTemplate HTTP 调用，异步线程执行，4 维 CompletableFuture 并发
- **文件存储**: MinIO（S3 兼容），预签名 URL 下载，PDFBox/POI 文档解析
- **部署**: Docker Compose 5 服务（MySQL/Redis/MinIO/Backend/Frontend）+ 阿里云 ACR + ECS

## Tech Stack

- **Backend**: Spring Boot 2.7.x + Java 8 + MyBatis-Plus + MySQL 5.6 (production DB)
- **Frontend**: Vue 3 + TypeScript + Element Plus + Pinia + ECharts
- **Infrastructure**: Docker Compose (MySQL 8.0 + Redis 7 + MinIO)
- **AI**: 火山引擎方舟 API (豆包/DeepSeek), HTTP REST with `RestTemplate`
- **Auth**: JWT (jjwt 0.11.5) + Spring Security + RBAC
- **File Storage**: MinIO (S3-compatible)

## Build & Run (Local Development)

```bash
# Backend (requires JDK 8, Maven 3.5+, local MySQL)
cd backend
mvn spring-boot:run                          # starts on :8080

# Frontend (requires Node 18+)
cd frontend
npm install && npm run dev                   # starts on :3000, proxies /api → :8080
```

**All-in-one Docker** (zero local dependency):
```bash
docker compose up -d --build                 # localhost:80 — frontend, :8080 — backend
```

## Docker Compose

`docker-compose.yml` defines 5 services: `mysql` (3307:3306), `redis` (6379), `minio` (9000+9001), `backend` (8080), `frontend` (80). Backend uses `SPRING_DATASOURCE_URL` env var to override DB connection in container. MinIO bucket `shuike-manager` must be created manually after first startup:
```bash
docker exec shuike-minio mc mb local/shuike-manager
```

## Tests

```bash
cd backend
mvn test -Dtest=ShuiKeIntegrationTest        # 23 API tests
mvn test -Dtest=ClosedLoopIntegrationTest    # 19 closed-loop E2E tests
mvn test                                      # all tests
```
Tests use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockBean(MinioClient.class)`. `RedisConfig` has `@ConditionalOnBean` so tests skip Redis. Tests connect to real local MySQL (`shuike_manager` database).

## Project Structure

```
backend/src/main/java/com/shuike/manager/
├── common/config/         # SecurityConfig, MyBatisPlusConfig, WebMvcConfig, AsyncConfig
├── common/security/       # JwtTokenProvider, JwtAuthenticationFilter, SecurityUtils
├── common/exception/      # GlobalExceptionHandler, BusinessException, ErrorCode
├── common/response/       # ApiResponse<T> (code/message/data/timestamp), PageResult<T>
├── modules/
│   ├── auth/              # Login + JWT refresh
│   ├── user/              # CRUD, batch import CSV, template download
│   ├── teachingplan/      # Legacy teaching plan (mostly deprecated)
│   ├── phasematerial/     # **CORE**: material CRUD, submit, review-list, archive
│   ├── review/            # Legacy review (mostly deprecated)
│   ├── manualreview/      # **CORE**: two-level review (COLLEGE→OFFICE)
│   ├── aievaluation/      # **CORE**: AI evaluation submit+run+query, Prompt template CRUD
│   ├── alignment/         # Industry demand alignment analysis (async LLM call)
│   ├── talentplan/        # Dean's talent cultivation plans (per-college isolation)
│   ├── coursestandard/    # Dean's course standards (per-college isolation)
│   ├── file/              # File upload to MinIO + document parsing (PDFBox/POI)
│   ├── notification/      # Notifications (CRUD + unread count)
│   └── dashboard/         # Stats for all 4 roles
│   └── ai/                # VolcanoEngineClient, PromptBuilder, EvaluationResultParser
frontend/src/
├── api/                   # Axios instance (interceptor: token + 401 refresh)
├── views/teacher/         # Dashboard, SubmitMaterial, ReviewProgress, AlignmentReport
├── views/college/         # Dashboard, MaterialReviewList (AI scoring + file preview)
├── views/office/          # Dashboard(ECharts), AiReviewManage, UserManage, MaterialArchive
├── views/dean/            # Dashboard, TalentPlan, CourseStandard, AlignmentReport
├── views/login/           # LoginPage
├── stores/                # auth.ts (token/userInfo/roles), app.ts (sidebar)
├── router/                # 4 role route trees + beforeEach guard
```

## API Pattern

All responses wrapped in `ApiResponse<T>`: `{code:200, message:"success", data:T, timestamp:123}`. Error codes: 200=success, 1001=wrong password, 4001=AI unavailable. Paginated results use `PageResult<T>`: `{records:[], total:N, page:N, pageSize:N}`.

## Key Architecture Decisions

1. **Two-level review flow**: material status: `AI_EVALUATING` → `AI_COMPLETED` → `COLLEGE_APPROVED` → `OFFICE_APPROVED` (final). Rejected at any stage → `AI_REJECTED`.
2. **College isolation**: `PhaseMaterialController.reviewerList()` filters by `teacher.college_id == currentUser.college_id` for COLLEGE_REVIEWER role. Office sees all.
3. **AI async execution**: `AiEvaluationService.submit()` inserts eval record, then uses `applicationContext.getBean()` + `new Thread()` to avoid `@Async` self-invocation failure. 4 dimensions called via `CompletableFuture.allOf()`.
4. **Volcano LLM call**: direct `RestTemplate` POST to `{endpoint}/chat/completions`. Max tokens configurable (6000 for alignment reports). Timeout 180s for alignment.
5. **MinIO file naming**: `phase-materials/{materialType}/{yyyy/MM}/{originalName}_{HHmmss}.ext`
6. **Prompt templates**: 4 material types × 4 dimensions = 16 specialized templates stored in `ai_prompt_templates` table. Dean can edit them online (auto-versioning).
7. **Material delete cascade**: deletes files from MinIO + AI evaluation records + phase_material record.

## Database

- Production: `mysql 5.6.26` running on local machine (docker-compose uses 8.0)
- Init scripts: `docs/database.sql` (schema + seed data) + `docs/prompt_data.sql` (16 prompt templates)
- Default accounts: `admin/123456`, `teacher1/123456`, `reviewer2/123456` (all roles)

## ECS Deployment

- Server: 47.97.68.38 (Aliyun ECS, CentOS 7, x86_64)
- Images pushed to ACR: `crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/teacher-file-manager-{backend,frontend}:1.0`
- ECS docker-compose: `/opt/shuike/docker-compose.yml`
- ECS SSH: `ssh root@47.97.68.38`, password: `118023203czH++`
- ACR login: `docker login --username=你啊空腹阿狸 crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com`

## Common Issues & Fixes

- **File upload failure on ECS**: MinIO bucket not created. Run `docker exec shuike-minio mc mb local/shuike-manager`
- **`semester_id` column missing**: `ALTER TABLE phase_materials ADD COLUMN semester_id BIGINT DEFAULT NULL AFTER course_id`
- **`material_id` NOT NULL**: `ALTER TABLE phase_material_files MODIFY material_id BIGINT DEFAULT NULL`
- **JWT key too short**: Secret must be ≥ 256 bits (32+ chars). Set via `JWT_SECRET` env var.
- **MySQL Chinese garbled**: Add `skip-character-set-client-handshake` in MySQL config + mount `mysql.cnf`
- **ARM image on x86 ECS**: Build with `--platform linux/amd64` locally. The `.dockerignore` excludes `target/` so ECS Dockerfile (Dockerfile.ecs) should not use `.dockerignore` or use a separate one.
- **ECS Docker Hub unreachable**: Configure `registry-mirrors` in `/etc/docker/daemon.json` with `docker.1ms.run` etc.

---

> **版本**: v1.0 | **更新日期**: 2026-06-11

---

## Superpowers-ZH 中文增强版

本项目已安装 superpowers-zh 技能框架（24 个 skills）。

### 核心规则

1. **收到任务时，先检查是否有匹配的 skill** — 哪怕只有 1% 的可能性也要检查
2. **设计先于编码** — 收到功能需求时，先用 brainstorming skill 做需求分析
3. **测试先于实现** — 写代码前先写测试（TDD）
4. **验证先于完成** — 声称完成前必须运行验证命令

### 可用 Skills

| Skill | 用途 |
|-------|------|
| brainstorming | 需求分析→设计规格，不写代码先想清楚 |
| using-superpowers | 元技能：确保每次对话前检查并调用匹配的 skills |
| writing-plans | 把规格拆成可执行的实施步骤 |
| executing-plans | 按计划逐步实施，每步验证 |
| test-driven-development | 严格 TDD：先写测试，再写代码 |
| systematic-debugging | 四阶段调试法：定位→分析→假设→修复 |
| requesting-code-review | 派遣审查 agent 检查代码质量 |
| receiving-code-review | 技术严谨地处理审查反馈 |
| verification-before-completion | 证据先行：声称完成前必须跑验证 |
| dispatching-parallel-agents | 多任务并发执行 |
| subagent-driven-development | 每个任务一个 agent，两轮审查 |
| using-git-worktrees | 隔离式特性开发 |
| finishing-a-development-branch | 合并/PR/保留/丢弃四选一 |
| writing-skills | 创建新 skill 的方法论 |
| chinese-code-review | 国内团队文化代码审查 |
| chinese-commit-conventions | 中文 Git 提交规范 |
| chinese-documentation | 中文技术文档写作规范 |
| chinese-git-workflow | Gitee/Coding/极狐 GitLab 工作流 |
| mcp-builder | 构建生产级 MCP 服务器 |
| workflow-runner | 多角色 YAML 工作流编排 |
| browser-use | 浏览器自动化 |
| cloud | 云端服务交互 |
| remote-browser | 远程浏览器控制 |

### 如何使用

当任务匹配某个 skill 时，使用 `Skill` 工具加载对应 skill 并严格遵循其流程。**绝不要用 Read 工具读取 SKILL.md 文件。**

如果你认为哪怕只有 1% 的可能性某个 skill 适用于你正在做的事情，你必须调用该 skill 检查。
