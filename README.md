# 水课管理系统 (ShuiKe Manager)

<p align="center">
  <img src="frontend/public/校徽.jpg" alt="校徽" width="80" height="80" style="border-radius:12px" />
</p>

<p align="center">
  <b>高校教学材料智能化分析与审核平台</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0-blue" alt="version">
  <img src="https://img.shields.io/badge/Java-1.8-orange" alt="java">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7-green" alt="spring">
  <img src="https://img.shields.io/badge/Vue-3.4-brightgreen" alt="vue">
  <img src="https://img.shields.io/badge/Docker-ready-blue" alt="docker">
  <img src="https://img.shields.io/badge/tests-42%2F42%20passed-success" alt="tests">
</p>

---

## 📖 项目简介

水课管理系统是一套面向高等院校的**教学材料智能化分析与审核平台**，覆盖授课计划、教案、课件、考核方案四种教学材料的全生命周期管理。系统集成**火山引擎大语言模型**，实现 AI 自动四维评审与优化建议生成，构建"教师提交→AI评审→学院主任审核→教务处终审"的完整业务闭环。

### 核心价值

- 🚀 **提升审核效率**：线上流转替代纸质审批，单次审核耗时从 3 天降至 1 天以内
- 🤖 **AI 智能评审**：大模型自动四维评分（内容完整性/课标匹配度/格式规范性/创新性），准确率 ≥ 85%
- 📊 **数据驱动决策**：多角色 Dashboard 可视化统计，学院隔离，精准掌握教学质量
- 🔒 **安全可靠**：RBAC 权限体系 + 数据隔离 + JWT 认证 + HTTPS 传输

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
│         JWT 认证 / RBAC 权限 / 全局异常处理                │
└────┬──────────┬──────────┬──────────┬───────────────────┘
     │          │          │          │
     ▼          ▼          ▼          ▼
┌─────────┐ ┌──────┐ ┌───────┐ ┌──────────────┐
│  MySQL  │ │Redis │ │ MinIO │ │ 火山引擎 API  │
│  8.0    │ │ 7.x  │ │ S3存储│ │ (豆包/DeepSeek)│
└─────────┘ └──────┘ └───────┘ └──────────────┘
```

---

## ✨ 核心功能

### 1. 教学材料提交与 AI 评审

| 材料类型 | AI 评审维度 | 权重 |
|----------|-----------|:--:|
| 授课计划 | 内容完整性 / 课标匹配度 / 格式规范性 / 创新性 | 30%+30%+20%+20% |
| 教案 | 同上（定制化 Prompt，精准评价课堂教学设计） | 同上 |
| 课件 | 同上（关注多媒体设计、交互创新） | 同上 |
| 考核方案 | 同上（聚焦评价科学性、考核方式合理性） | 同上 |

- 4 个维度**并发调用**大模型，90 秒内完成全部评审
- 16 套专业 Prompt 模板，支持**在线编辑 + 自动版本管理**
- 支持 Word/DOCX、PDF 全文解析，文本内容自动缓存

### 2. 两级审核流程

```
教师提交 → AI评审中 → 待主任审核 → 待教务处终审 → 已通过
                ↘ 驳回修改              ↘ 驳回修改
```

- **学院主任**：查看 AI 评分详情，在线预览材料原文，通过/驳回（含修改要求）
- **教务处**：终审确认，材料归档
- 审核结果实时推送至教师通知中心

### 3. 多角色工作台

| 角色 | 专属功能 | Dashboard 统计 |
|------|---------|:--:|
| **教师** | 提交材料、查看审核进度、对齐分析报告 | 材料统计卡片 |
| **学院主任** | 审核材料（含 AI 评分+文件预览下载）、级联筛选 | ECharts 5 张图表（类型分布/教师对比/评分排行） |
| **教务处** | 终审、AI Prompt 管理、用户管理、材料归档、批量导入 | ECharts 7 张图表（含分数分布饼图/学院统计/审核流转面积图） |
| **院长** | 人培方案管理、课程标准管理、产业需求对齐分析 | 人培/课标总数 |

### 4. 产业需求对齐分析

- **AI 生成 8 章节论文式报告**：专业概况→产业背景→人才需求→课程体系→学习成果→就业前景→差距分析→改进建议
- 含 6 张 ECharts 交互图表（岗位需求柱状图、技能雷达图、课程评分雷达图等）
- 教师可查看本院报告（只读），指导授课计划制定

### 5. 其他亮点功能

- 📢 **通知中心**：全流程实时通知，铃铛角标 30s 自动刷新
- 📥 **CSV 批量导入用户**：密码默认 `fzrjxy+工号`，学院识别支持名称或 ID
- 📁 **材料归档**：按类型/课程/教师多维筛选，Word/PDF 在线预览，支持批量下载
- 🗑️ **安全删除**：删除材料同步清理 MinIO 文件 + AI 评审记录
- 🔐 **数据隔离**：主任仅见本院、教师仅见个人、院长仅见本学院

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot + Spring Security + JWT | 2.7.x |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis | 7.x |
| **文件存储** | MinIO (S3 兼容) | latest |
| **AI 引擎** | 火山引擎方舟 API (豆包/DeepSeek) | v3 |
| **文档解析** | Apache PDFBox + POI | 2.0.30 / 5.2.5 |
| **前端框架** | Vue 3 + TypeScript | 3.4+ |
| **UI 组件库** | Element Plus | 2.7+ |
| **状态管理** | Pinia | 2.x |
| **图表** | ECharts | 5.x |
| **构建工具** | Vite | 5.x |
| **容器化** | Docker + Docker Compose | — |
| **部署平台** | 阿里云 ECS + ACR | — |

---

## 🚀 快速开始

### 前置条件

- JDK 8 + Maven 3.5+
- Node.js 18+
- Docker Desktop (可选，推荐)
- MySQL 5.6+ (本地开发)

### 本地开发

```bash
# 1. 导入数据库
mysql -u root -p < docs/database.sql
mysql -u root -p --default-character-set=utf8 < docs/prompt_data.sql

# 2. 启动后端
cd backend
mvn spring-boot:run
# → http://localhost:8080

# 3. 启动前端
cd frontend
npm install && npm run dev
# → http://localhost:3000
```

### Docker 一键部署

```bash
# 启动全部 5 个服务 (MySQL + Redis + MinIO + Backend + Frontend)
docker compose up -d --build

# 创建 MinIO 存储桶（仅首次）
docker exec shuike-minio mc mb local/shuike-manager

# 导入 Prompt 数据（仅首次）
docker exec -i shuike-mysql mysql -u root -pshuike@2026 --default-character-set=utf8 < docs/prompt_data.sql

# → http://localhost  (前端)
# → http://localhost:9001 (MinIO 控制台)
```

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | 教务处 |
| `teacher1` | `123456` | 教师 |
| `reviewer2` | `123456` | 学院主任 |
| `dean1` | `123456` | 院长 |

---

## 📁 项目结构

```
shuike-manager/
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/shuike/manager/
│   │   ├── common/                  # 公共模块（安全/异常/响应/AOP）
│   │   └── modules/                 # 业务模块
│   │       ├── auth/                # 认证
│   │       ├── phasematerial/       # 核心：材料管理
│   │       ├── aievaluation/        # 核心：AI 评审 + Prompt 管理
│   │       ├── manualreview/        # 核心：两级审核
│   │       ├── alignment/           # 对齐分析
│   │       ├── talentplan/          # 人培方案
│   │       ├── coursestandard/      # 课程标准
│   │       ├── notification/        # 通知
│   │       ├── file/                # 文件上传 + 文档解析
│   │       ├── dashboard/           # Dashboard 统计
│   │       ├── ai/                  # LLM 客户端 + Prompt 构建 + 结果解析
│   │       └── user/                # 用户管理
│   ├── src/test/                    # 42 个集成测试
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                        # Vue 3 前端
│   ├── src/
│   │   ├── views/teacher/           # 教师端 8 页
│   │   ├── views/college/           # 主任端 3 页
│   │   ├── views/office/            # 教务处端 10 页
│   │   ├── views/dean/              # 院长端 6 页
│   │   ├── api/                     # Axios API 层
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── router/                  # 4角色路由
│   │   └── layouts/                 # 布局组件
│   ├── Dockerfile
│   └── nginx.conf
├── docs/                            # 文档
│   ├── PRD-水课管理系统.md           # 产品需求文档
│   ├── 前端设计.md                   # 前端详细设计
│   ├── 后端设计.md                   # 后端详细设计
│   ├── database.sql                 # 数据库初始化
│   ├── prompt_data.sql              # Prompt 模板数据
│   ├── 项目上线部署手册.md            # 部署指南
│   ├── 闭环测试报告.md               # 42用例测试报告
│   └── 软件测试报告.md               # 23用例测试报告
├── docker-compose.yml               # Docker 编排
├── mysql.cnf                        # MySQL 编码配置
├── README.md
└── CLAUDE.md
```

---

## 🧪 自动化测试

```bash
# 运行全部 42 个测试用例
cd backend && mvn test

# 功能测试 (23 个)
mvn test -Dtest=ShuiKeIntegrationTest

# 闭环测试 (19 个)
mvn test -Dtest=ClosedLoopIntegrationTest
```

**测试结果**: `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0` ✅

覆盖：登录/权限/材料提交/审核/AI评审/通知/Dashboard/对齐分析/CRUD/边界条件

---

## 📊 项目统计

| 维度 | 数据 |
|------|------|
| Java 文件 | 96 个 |
| Vue 页面 | 33 个 |
| 数据库表 | 19 张 |
| REST API 接口 | 50+ 个 |
| Prompt 模板 | 16 个 (4 类型 × 4 维度) |
| 测试用例 | 42 个 (100% 通过) |
| Docker 服务 | 5 个 |
| 用户角色 | 4 个 |

---

## 🌐 线上地址

- **生产环境**: http://47.97.68.38
- **MinIO 控制台**: http://47.97.68.38:9001
- **镜像仓库**: `crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/`

---

## 📝 更新日志

### v1.0 (2026-06-11)

- ✅ 完整的教师提交→AI评审→两级审核流程
- ✅ 火山引擎大模型四维并发评审
- ✅ 4 角色多工作台 + ECharts 可视化Dashboard
- ✅ 16 模板 Prompt 管理系统
- ✅ 产业需求对齐分析（论文式 8 章节报告）
- ✅ 通知中心 + 材料归档 + 批量导入
- ✅ Docker Compose 一键部署
- ✅ 42 个自动化测试全通过
- ✅ 阿里云 ECS 生产环境上线

---

## 👨‍💻 开发者

- **GitHub**: [chenzh-lgtm](https://github.com/chenzh-lgtm)
- **仓库**: https://github.com/chenzh-lgtm/shuike-manager

---

> **版本**: v1.0 | **更新时间**: 2026-06-12
