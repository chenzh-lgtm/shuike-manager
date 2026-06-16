# 水课管理系统 (ShuiKe Manager)

<p align="center">
  <img src="frontend/public/校徽.jpg" alt="校徽" width="80" height="80" style="border-radius:12px" />
</p>

<p align="center">
  <b>高校教学材料智能化分析与审核平台</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-2.3-blue" alt="version">
  <img src="https://img.shields.io/badge/Java-1.8-orange" alt="java">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7-green" alt="spring">
  <img src="https://img.shields.io/badge/Vue-3.4-brightgreen" alt="vue">
  <img src="https://img.shields.io/badge/Docker-ready-blue" alt="docker">
  <img src="https://img.shields.io/badge/tests-48%2F48%20passed-success" alt="tests">
  <img src="https://img.shields.io/badge/PRD-94.5%25-brightgreen" alt="prd">
  <img src="https://img.shields.io/badge/license-MIT-yellow" alt="license">
</p>

---

## 📖 项目简介

水课管理系统是一套面向高等院校的**教学材料智能化分析与审核平台**，覆盖授课计划、教案、课件、考核方案四种教学材料的全生命周期管理。系统集成**火山引擎大语言模型**（豆包/DeepSeek），实现 AI 自动五维评审（含 AI 生成检测）与优化建议生成，构建"教师提交→AI评审→学院主任审核→教务处终审"的完整业务闭环。

### 项目定位

| 维度 | 说明 |
|------|------|
| **目标用户** | 高校教师、学院主任、教务处、院长 |
| **核心痛点** | 教学材料审核量大、评审标准不统一、缺乏产业需求对齐分析 |
| **解决方案** | AI 大模型五维并发评审 + 多级在线审核流程 + 产业需求对齐论文式报告 |
| **交付形态** | Docker Compose 一键部署的 Web 应用，适配阿里云 ECS 生产环境 |

### 核心价值

- 🚀 **提升审核效率**：线上流转 + 批量审核，单次审核从传统 3 天降至 1 天以内
- 🤖 **AI 智能评审**：火山引擎大模型自动五维评分（内容完整性28%/课标匹配度28%/格式规范性18%/创新性16%/AI生成检测10%），含反注入过滤
- 📊 **数据驱动决策**：4 角色专属 ECharts 可视化 Dashboard，学院数据隔离，精准掌握教学质量
- 🔒 **安全可靠**：RBAC 四角色权限 + 学院级隔离 + 操作日志全覆盖 + AI 内容脱敏

### 项目规模

| 维度 | 数据 | 维度 | 数据 |
|------|:--|------|:--|
| Java 文件 | 100+ 个 | Vue 页面 | 35 个 |
| 数据库表 | 19 张 | REST API 端点 | 60+ 个 |
| Prompt 模板 | 16 条 v2.0 专业版 | 自动化测试用例 | 48 个（100% 通过） |
| Docker 服务 | 5 个 | 用户角色 | 4 个 |
| 技术文档 | 10+ 份 | Controller | 17 个（15个有操作日志） |

---

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────────┐
│                      用户层                               │
│   教师端 (提交+查看进度)  │  主任端 (审核+统计)             │
│   教务处端 (终审+归档)   │  院长端 (人培+产业分析)           │
└────────────────────────┬─────────────────────────────────┘
                         │  HTTPS
┌────────────────────────▼─────────────────────────────────┐
│              Nginx 反向代理 + Vue 3 静态资源               │
│                   (端口 80)                               │
└────────────────────────┬─────────────────────────────────┘
                         │  /api 代理
┌────────────────────────▼─────────────────────────────────┐
│              Spring Boot 2.7 REST API                    │
│   JWT 认证 / RBAC 权限 / AOP 操作日志 / 全局异常处理       │
└────┬──────────┬──────────┬──────────┬───────────────────┘
     │          │          │          │
     ▼          ▼          ▼          ▼
┌─────────┐ ┌──────┐ ┌───────┐ ┌──────────────┐
│  MySQL  │ │Redis │ │ MinIO │ │ 火山引擎 API  │
│  8.0    │ │ 7.x  │ │ S3存储 │ │     (豆包)    │
└─────────┘ └──────┘ └───────┘ └──────────────┘
```

### Docker Compose 服务编排

| 服务 | 镜像 | 端口 | 说明 |
|------|------|:--|------|
| `mysql` | mysql:8.0 | 3307→3306 | 业务数据库，自动初始化 schema |
| `redis` | redis:7-alpine | 6379:6379 | 缓存层 |
| `minio` | minio/minio | 9000 + 9001 | 文件存储（S3 兼容） |
| `backend` | shuike-backend:1.0 | 8080:8080 | Spring Boot API 服务 |
| `frontend` | shuike-frontend:1.0 | 80:80 | Nginx + Vue 静态资源 |

---

## ✨ 核心功能

### 1. AI 五维并发评审 ⭐核心创新

使用 `CompletableFuture.allOf()` 并发调用大模型，30-90 秒内完成全部评审。v2.3 新增**第5维度"AI生成检测"**（本地算法，检测AI生成文本特征）和**反注入过滤**（防止"给我100分"等提示词注入）。

| 评审维度 | 权重 | 评审重点 |
|----------|:--:|------|
| 内容完整性 | 28% | 知识点覆盖、教学目标、教学环节 |
| 课标匹配度 | 28% | 与课程标准对齐程度 |
| 格式规范性 | 18% | 文档排版、引用规范 |
| 创新性 | 16% | 教学方法创新、新技术应用 |
| **AI生成检测** ⭐v2.3 | 10% | 检测材料是否AI生成（本地算法） |

| 指标 | 数据 |
|------|:--|
| 评审速度 | 30-60 秒 / 份 |
| 并发维度 | 4 维 LLM + 1 维本地 = 5 维 |
| Prompt 模板 | 16 条 v2.0 专业版（4 类型 × 4 维度） |
| 安全防护 | ContentSanitizer 反注入 + AI生成检测 |

### 2. 两级审核闭环（含批量审核） ⭐v2.3

```
教师提交 → AI评审中 → 待主任审核 → 待教务处终审 → 已通过（归档）
                ↘ 驳回修改              ↘ 驳回修改
                     ↘ 教师重传（重置状态+重新AI评审）
```

- **批量审核**：主任端勾选多条材料一键通过（v2.3 新增）
- **AI评分筛选**：主任端按 AI 评分区间过滤材料（v2.3 新增）
- **驳回修改**：必填修改要求+截止日期，教师收到通知后可重传
- **导出 Excel**：主任/教务处导出已通过名单（CSV UTF-8），学院隔离（v2.3 新增）

### 3. 多角色专属工作台

| 角色 | 页面数 | 核心功能 |
|------|:--:|------|
| **教师** | 8 | 提交 4 类材料、AI 评分反馈、审核进度追踪、产业分析报告（含 PDF 导出） |
| **学院主任** | 3 | 审核本院材料（批量+AI评分筛选）、导出已通过名单 |
| **教务处** | 12 | 终审、材料归档导出、AI Prompt 管理、用户管理、系统配置、操作日志查看 |
| **院长** | 6 | 人培方案+指标点映射矩阵、课程标准、对齐分析（含 PDF 导出） |

### 4. 产业需求对齐分析（支持 PDF 导出） ⭐v2.3

院长发起后，LLM 基于人培方案全文，生成 **8 章论文式报告**，支持浏览器打印为 PDF。

### 5. Prompt 模板管理系统

教务处可在线编辑 16 条 v2.0 专业 Prompt 模板，支持自动版本管理，每条含专业人设+分值分配明细。

### 6. 系统配置管理 ⭐v2.2

教务处在线管理 11 项系统参数，审核流程可动态切换（关闭学院审核后 AI 评审直通教务处终审）。

### 7. 操作日志系统 ⭐v2.2

AOP 自动记录 15 个 Controller、35 个方法的全部操作，人类可读格式（"张三 创建了「教学材料」"），支持按模块/类型/时间筛选查询。

### 8. 更多功能

| 功能 | 说明 |
|------|------|
| 📢 **通知中心** | 全流程自动推送，铃铛角标 30s 自动刷新 |
| 📥 **CSV 批量导入** | 支持 CSV 一键导入用户，密码默认规则 `fzrjxy+工号` |
| 📁 **材料归档** | 按类型/课程/教师筛选，Word/PDF 在线预览，批量下载+导出Excel |
| 🔐 **数据隔离** | 主任仅见本院材料、教师仅见个人、导出Excel含学院隔离 |
| 🛡️ **AI 安全** | ContentSanitizer 反注入过滤 + AI生成检测维度（v2.3） |
| 📄 **PDF 导出** | 对齐分析报告一键导出为 PDF（v2.3） |
| 🔄 **材料重传** | 驳回后教师可重传材料，重置状态并重新触发 AI 评审（v2.3） |
| 🎯 **指标点映射矩阵** | 院长端课程×培养目标矩阵表格录入（v2.3） |

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|------|
| **后端框架** | Spring Boot + Spring Security + JWT | 2.7.x | 兼容 Java 8 生产环境 |
| **ORM** | MyBatis-Plus | 3.5.5 | 简化 CRUD，Lambda 条件构造 |
| **数据库** | MySQL | 8.0 (开发) / 5.6 (生产) | 生产为阿里云 ECS 原生 MySQL 5.6 |
| **缓存** | Redis | 7.x | 条件化配置，测试自动跳过 |
| **文件存储** | MinIO (S3 兼容) | latest | 按材料类型分目录，预签名 URL |
| **AI 引擎** | 火山引擎方舟 API | v3 | RestTemplate HTTP 调用 |
| **文档解析** | Apache PDFBox + POI | 2.0.30 / 5.2.5 | Word(.doc/.docx) + PDF |
| **前端框架** | Vue 3 + TypeScript | 3.4+ | Composition API |
| **UI 组件库** | Element Plus | 2.7+ | 中文友好 |
| **状态管理** | Pinia | 2.x | Vue 3 官方推荐 |
| **图表** | ECharts | 5.x | 多角色 Dashboard 12+ 张图表 |
| **构建工具** | Vite | 5.x | 快速 HMR |
| **容器化** | Docker + Docker Compose | — | 5 服务一键部署 |
| **部署平台** | 阿里云 ECS | — | CentOS 7 x86_64 |

---

## 📁 项目结构

```
shuike-manager/
├── backend/                                  # Spring Boot 后端
│   ├── src/main/java/com/shuike/manager/
│   │   ├── common/                           # 公共模块
│   │   │   ├── config/                       # SecurityConfig, MyBatisPlusConfig, AsyncConfig...
│   │   │   ├── security/                     # JWT + SecurityUtils
│   │   │   ├── exception/                    # GlobalExceptionHandler, ErrorCode
│   │   │   ├── response/                     # ApiResponse<T>, PageResult<T>
│   │   │   ├── aspect/                       # OperationLog AOP（35方法覆盖）
│   │   │   └── constant/                     # PlanStatusEnum, RoleEnum
│   │   └── modules/
│   │       ├── auth/                         # 认证登录 + JWT刷新 + 修改密码
│   │       ├── user/                         # 用户管理 + CSV批量导入
│   │       ├── college/                      # 学院管理
│   │       ├── course/                       # 课程管理
│   │       ├── semester/                     # 学期管理
│   │       ├── phasematerial/                # 🔴 材料CRUD + 审核列表 + 归档 + 批量审核 + 导出Excel + 重传
│   │       ├── aievaluation/                 # 🔴 AI评审 + Prompt模板管理
│   │       ├── manualreview/                 # 🔴 两级审核（COLLEGE→OFFICE）+ 驳回通知
│   │       ├── alignment/                    # 对齐分析 + PDF导出
│   │       ├── talentplan/                   # 人培方案 + 指标点映射矩阵
│   │       ├── coursestandard/               # 课程标准管理
│   │       ├── notification/                 # 通知中心
│   │       ├── dashboard/                    # 4角色Dashboard统计
│   │       ├── file/                         # 文件上传/解析/预览/下载
│   │       ├── operationlog/                 # 🔴 操作日志查询⭐v2.2
│   │       ├── systemconfig/                 # 🔴 系统配置管理⭐v2.2
│   │       ├── ai/                           # VolcanoEngineClient + PromptBuilder + ContentSanitizer
│   │       ├── teachingplan/                 # 教学计划（旧版兼容）
│   │       └── review/                       # 审核（旧版兼容）
│   ├── src/test/                             # 48 个集成测试
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                                 # Vue 3 前端
│   ├── src/
│   │   ├── views/teacher/                    # 教师端 8 页
│   │   ├── views/college/                    # 主任端 3 页（批量审核+导出）
│   │   ├── views/office/                     # 教务处端 12 页（含操作日志+系统配置）
│   │   ├── views/dean/                       # 院长端 6 页（映射矩阵+PDF导出）
│   │   ├── views/login/                      # 登录页
│   │   ├── api/                              # Axios 封装 + API 模块
│   │   ├── stores/                           # Pinia (auth/app)
│   │   ├── router/                           # 4 角色路由 + beforeEach 守卫
│   │   └── layouts/                          # MainLayout + 侧边栏
│   ├── nginx.conf                            # 反向代理 /api → backend:8080
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml                        # 5 服务编排
├── mysql.cnf
├── docs/                                     # 技术文档
│   ├── prd需求分析/                           # PRD 产品需求文档
│   ├── 数据库/                                # database.sql + prompt_data.sql
│   ├── 测试报告/                              # 综合测试报告 + 闭环测试 + PRD验收测试
│   ├── API.md                                # API 完整文档
│   └── pitch-deck/                           # 路演网页版 PPT
├── screenshots/                              # 系统截图
├── README.md
└── CLAUDE.md
```

---

## 🚀 快速开始

### 前置条件

| 工具 | 版本要求 | 说明 |
|------|------|------|
| JDK | 8+ | 本地开发编译后端 |
| Maven | 3.5+ | 依赖管理和构建 |
| Node.js | 18+ | 前端开发 |
| Docker Desktop | — | 推荐，一键启动全部服务 |

### 本地开发

```bash
# 1. 导入数据库
mysql -u root -p < docs/数据库/database.sql
mysql -u root -p --default-character-set=utf8 < docs/数据库/prompt_data.sql

# 2. 启动后端 (JDK 8)
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install && npm run dev
```

### Docker 一键部署

```bash
docker compose up -d --build
docker exec shuike-minio mc mb local/shuike-manager  # 首次创建桶
docker exec -i shuike-mysql mysql -u root -pshuike@2026 \
  --default-character-set=utf8 < docs/数据库/prompt_data.sql  # 首次导入模板
```

### 默认账号

| 用户名 | 密码 | 角色 | 所属学院 |
|--------|------|------|---------|
| `office1` | `123456` | 教务处 | 教务处 |
| `teacher1` | `123456` | 教师 | 计算机学院 |
| `teacher2` | `123456` | 教师 | 数学学院 |
| `reviewer2` | `123456` | 专业主任 | 计算机学院 |
| `reviewer3` | `123456` | 专业主任 | 数学学院 |
| `dean1` | `123456` | 院长 | 计算机学院 |
| `dean2` | `123456` | 院长 | 数学学院 |

---

## 📝 需求迭代记录

### v2.3 (2026-06-16) — 当前版本

- ✅ 批量审核（主任端勾选通过）+ AI评分区间筛选
- ✅ 导出已通过名单Excel（主任/教务处，学院隔离）
- ✅ 对齐分析报告 PDF 导出（院长+教师端）
- ✅ 指标点映射矩阵前端 UI（课程×目标 0-5分值）
- ✅ AI 反注入过滤（ContentSanitizer 7种模式）
- ✅ AI 生成检测维度（第5维度，本地算法，权重10%）
- ✅ 驳回材料重传（重置状态+重新触发AI评审）
- ✅ 操作日志系统（AOP 15个Controller 35方法覆盖）
- ✅ 系统配置管理（11项在线管理，审核流程可动态切换）
- ✅ PRD 验收通过率 94.5%（48 测试用例）

### v2.2 (2026-06-15)

- ✅ Prompt 模板升级为 16 条 v2.0 专业版
- ✅ 驳回通知含修改要求+截止日期
- ✅ 教师端课程学院隔离
- ✅ 操作日志/系统配置模块

### v2.1 (2026-06-14)

- ✅ Dashboard 图表优化
- ✅ 路演网页版 PPT
- ✅ API 文档完整输出

### v2.0 (2026-06-12)

- ✅ 两级审核流程 + 真实 LLM 并发评审
- ✅ Prompt 模板管理系统
- ✅ 产业需求对齐分析（8章报告）
- ✅ 通知中心 + 材料归档 + CSV批量导入
- ✅ Docker Compose 一键部署

---

## 🧪 自动化测试

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home
mvn test                                         # 全部 48 个
mvn test -Dtest=ShuiKeIntegrationTest            # 29 个 API 测试
mvn test -Dtest=ClosedLoopIntegrationTest        # 19 个 E2E 测试
```

**测试结果**: `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0` ✅

---

## 🌐 线上环境

| 配置项 | 值 |
|--------|-----|
| 生产地址 | 47.97.68.38 |
| 部署路径 | /opt/shuike/ |
| ACR 仓库 | crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com |

---

## 👨‍💻 开发者

- **GitHub**: [chenzh-lgtm](https://github.com/chenzh-lgtm)
- **技术栈**: Spring Boot 2.7 + Vue 3 + MySQL + Redis + MinIO + Docker
- **AI 引擎**: 火山引擎方舟 API（豆包/DeepSeek）

---

> **版本**: v2.3 | **更新时间**: 2026-06-16 | **接口总数**: 60+ | **测试覆盖**: 48/48 ✅ | **PRD验收**: 94.5%
