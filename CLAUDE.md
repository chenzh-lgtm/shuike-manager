# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**项目规则**: 请同时遵守 [.claude/rules/shuike-manager.md](.claude/rules/shuike-manager.md) 中的全部规则。
**Skills**: 本项目已安装 27 个 skills（superpowers-zh 23 + html-ppt 4），全部在 `.claude/skills/`。mattpocock 技能已全部移除。

---

## 环境约束（强制）

- **macOS 开发，不是 Linux**。ECS 生产环境是 CentOS 7 x86_64。
- **Java 8**：不能使用 `var`、`List.of()`、模块系统、`te?xt blocks` 等 9+ 特性。
- **MySQL 5.6 兼容**：生产是原生 MySQL 5.6.26（ECS 宿主机），不能用 `JSON` 列类型、窗口函数、CTE。本地 Docker 是 MySQL 8.0，开发时注意语法降级。
- **Maven 3.5.2**：插件版本需兼容。
- 工具链（node/npm/maven/git/java）已全部安装，**不要重复安装**。

## 双数据库版本（关键）

| 环境 | MySQL 版本 | Schema 来源 |
|------|:--:|------|
| 本地 Docker | 8.0 | `docs/数据库/database.sql`（挂载在 docker-compose.yml） |
| ECS 生产 | 5.6.26 | **手动维护**，ALTER TABLE 增量更新 |

> ECS 数据库不会自动执行建表脚本。任何 schema 变更必须**同时在 ECS 上 `ALTER TABLE`**。

已知 ECS 已执行过的补丁：
- `ALTER TABLE phase_materials ADD COLUMN semester_id BIGINT DEFAULT NULL AFTER course_id`
- `ALTER TABLE phase_material_files MODIFY material_id BIGINT DEFAULT NULL`
- `ALTER TABLE alignment_reports ADD COLUMN report_json LONGTEXT DEFAULT NULL AFTER suggestions`
- `ALTER TABLE ai_prompt_templates ADD COLUMN material_type VARCHAR(30) DEFAULT NULL AFTER scene`
- `INSERT INTO system_configs (config_key, config_value, description) VALUES ('enable_college_review', 'true', '是否启用学院审核环节') ON DUPLICATE KEY UPDATE config_value=config_value`

## 后端快速部署（日常使用）

```bash
# 1. 本地编译（需要正确 JAVA_HOME，30秒）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home
cd backend && mvn clean package -DskipTests -q

# 2. 上传 JAR + 替换容器（1分钟）
scp target/teacher-file-manager-1.0.0.jar root@47.97.68.38:/opt/shuike/backend/
ssh root@47.97.68.38 '
  docker cp /opt/shuike/backend/teacher-file-manager-1.0.0.jar shuike-backend:/app/app.jar
  docker restart shuike-backend
  sleep 15 && docker logs shuike-backend --tail 5
'
```

> ⚠️ **不要用 `docker build --platform linux/amd64` 在 Mac 上构建后端**（QEMU 模拟 Java 编译需 3h+）。本地 mvn 编译后直接替换容器 JAR 即可。

## 前端部署

```bash
cd frontend
npx vite build                            # 本地构建（7秒）
docker build --platform linux/amd64 -t shuike-frontend:1.0 .
docker save shuike-frontend:1.0 | gzip > /tmp/shuike-frontend.tar.gz
scp /tmp/shuike-frontend.tar.gz root@47.97.68.38:/opt/shuike/
ssh root@47.97.68.38 '
  cd /opt/shuike
  gunzip -c shuike-frontend.tar.gz | docker load
  docker tag shuike-frontend:1.0 crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/teacher-file-manager-frontend:1.0
  docker compose up -d --no-deps frontend
'
```

> ⚠️ ECS compose 使用 ACR 镜像名 `crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/teacher-file-manager-frontend:1.0`，部署前必须 `docker tag` 覆盖，否则会拉取远程旧镜像。

## 本地 Docker 同步

修改代码后本地 Docker 也需更新（否则过时）：

```bash
docker compose up -d --no-deps --build backend frontend
```

如果 Dashboard 图表全空，检查数据库是否初始化：`docker-compose.yml` 中挂载的是 `./docs/数据库/database.sql`（已修复，旧版引用的 `./docs/database.sql` 在文档重组后失效）。

## 架构要点

### 核心两个版本模块共存

- **新版（主用）**：`phasematerial/` (材料) + `aievaluation/` (AI评审) + `manualreview/` (两级审核)
- **旧版（兼容）**：`teachingplan/` + `review/` — 单级审核，大部分接口仍可用但前端已不再使用

### 审核状态机

```
AI_EVALUATING → AI_COMPLETED → COLLEGE_APPROVED → OFFICE_APPROVED (终审通过)
                                     ↘ AI_REJECTED (任意阶段驳回)
```

### @Async + SecurityContext 规范

异步方法（`@Async("aiEvaluationExecutor")`）在独立线程执行，**不能调用 `SecurityUtils.getCurrentUserId()`**（返回 null）。正确做法：

```java
// Controller 层提前取 userId 显式传入
Long userId = SecurityUtils.getCurrentUserId();
service.analyzeAsync(tcpId, userId);   // ✅ 显式传参

// Service 层异步方法接收参数
@Async("aiEvaluationExecutor")
public void analyzeAsync(Long tcpId, Long initiatorId) {  // ✅ 使用传入的 initiatorId
```

### 数据隔离模式

- **COLLEGE_REVIEWER**：`PhaseMaterialController.reviewerList()` 中通过 `teacherCollegeCache` 过滤出 `userCollegeId.equals(tcId)` 的记录
- **DEAN**：人培/课标查询中设置 `collegeId = currentUser.getCollegeId()`
- **OFFICE**：全局视角，无隔离；API 接收可选 `collegeId` 参数筛选

### API 响应格式

`ApiResponse<T>`: `{code:200, message:"success", data:T, timestamp:123}`
`PageResult<T>`: `{records:[], total:N, page:N, pageSize:N}`

常见错误码：200=成功, 1001=密码错误, 4001=AI 不可用, 4003=AI 结果解析失败

## 核心模块速查

| 需求 | Controller | Service | 关键点 |
|------|-----------|---------|------|
| 教师提交材料 | `PhaseMaterialController.create()` | `PhaseMaterialService` | 创建后自动触发 AI 评审（异步），冲突检测自动取消旧记录 |
| 主任/教务处审核 | `ManualReviewController.review()` | `ManualReviewService` | `reviewLevel:COLLEGE/OFFICE`，可配置 `enable_college_review` |
| AI 评审 | `AiEvaluationController.submit()` | `AiEvaluationService` | 5维（4维LLM并发+1维本地AI生成检测），ContentSanitizer 反注入 |
| 对齐分析 | `AlignmentController.analyze()` | `AlignmentService` | `@Async`，LLM 返回 JSON 解析后入库，支持 PDF 导出 |
| Prompt 模板 | `PromptTemplateController` | `PromptTemplateService` | 16条v2.0专业版，在线编辑自动创建新版本 |
| 文件管理 | `FileController` | — | MinIO 预签名 URL + PDFBox/POI 解析 |
| Dashboard | `DashboardController.office()` | `DashboardService.getOfficeStats()` | `collegeMaterialAvgScores` 各学院×材料类型平均分 |
| 操作日志 | `OperationLogController` | `OperationLogService` | AOP 15个Controller 35方法覆盖，人类可读格式 |
| 系统配置 | `SystemConfigController` | `SystemConfigService` | 11项在线管理，审核流程动态切换 |
| 批量审核 | `PhaseMaterialController.batchReview()` | — | 主任端勾选通过，`POST /reviewer/batch-review` |
| 导出Excel | `PhaseMaterialController.exportApproved()` | — | CSV UTF-8 BOM，主任/教务处，学院隔离 |
| 材料重传 | `PhaseMaterialController.resubmit()` | `PhaseMaterialService` | 驳回→重传→重置状态→重新AI评审 |

## ECS 运维速查

```
SSH: ssh root@47.97.68.38
项目路径: /opt/shuike/

docker logs shuike-backend --tail 100 | grep -i "对齐\|ERROR"
docker exec shuike-mysql mysql -uroot -p'shuike@2026' -e "DESCRIBE shuike_manager.alignment_reports"
docker exec shuike-mysql mysql -uroot -p'shuike@2026' -e "SELECT id,major_name,coverage_score FROM shuike_manager.alignment_reports ORDER BY created_at DESC LIMIT 5"
docker restart shuike-backend
docker compose up -d --no-deps frontend  # 只重建前端
```

ACR: `crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/`

## 测试

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home
cd backend
mvn test                                         # 全部 48 个
mvn test -Dtest=ShuiKeIntegrationTest            # 29 个 API 测试
mvn test -Dtest=ClosedLoopIntegrationTest        # 19 个 E2E 测试
```

测试使用 `@SpringBootTest` + `@MockBean(MinioClient.class)`，Redis 通过 `@ConditionalOnBean` 自动跳过，连接真实本地 MySQL。

## 文档索引

| 文档 | 路径 |
|------|------|
| PRD 需求分析 | `docs/prd需求分析/` |
| API 完整文档 | `docs/API.md` |
| 数据库 Schema | `docs/数据库/` |
| Prompt 模板数据 | `docs/数据库/prompt_data.sql` |
| 部署修复指南 | `docs/测试报告/部署问题修复指导.md` |
| 需求版本管理 | `docs/需求版本管理-v2.2.md` |
| PRD 验收测试 | `docs/测试报告/PRD验收测试报告-v2.3.md` |
| 项目配置手册 | `docs/项目配置手册.md` |
| 路演 PPT | `docs/pitch-deck/index.html` |

---

> **更新**: 2026-06-16 | **版本**: v2.3 | **测试**: 48/48 ✅ | **PRD验收**: 94.5%
