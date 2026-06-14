# 水课管理系统 — CI/CD 配置说明

> **日期**: 2026-06-15 | **工具**: GitHub Actions + 阿里云 ACR + ECS

---

## 1. 工作流概览

| 工作流 | 文件 | 触发条件 | 任务 |
|--------|------|------|------|
| **CI** | `.github/workflows/ci.yml` | push / PR 到 `main` | 后端编译 + 42个测试 + 前端编译 |
| **CD** | `.github/workflows/cd.yml` | push `main`（仅代码变更时）、手动 `workflow_dispatch` | 测试 → 构建镜像 → 推送 ACR → 部署到 ECS |

### 流程图

```
Git Push → CI 编译+测试 → CD 构建镜像
                ↓                     ↓
          PR 阻断(测试失败)    推送阿里云 ACR
                                     ↓
                              SSH 到 ECS 重启容器
```

---

## 2. GitHub Secrets 配置

在 GitHub 仓库 → Settings → Secrets and variables → Actions 中添加以下 Secrets：

| Secret | 值 | 说明 |
|--------|-----|------|
| `ACR_USERNAME` | `你啊空腹阿狸` | 阿里云 ACR 用户名 |
| `ACR_PASSWORD` | `118023203czH++` | 阿里云 ACR 密码 |
| `ECS_HOST` | `47.97.68.38` | ECS 服务器 IP |
| `ECS_USERNAME` | `root` | ECS SSH 用户名 |
| `ECS_PASSWORD` | `118023203czH++` | ECS SSH 密码 |

### 配置步骤

1. 打开 https://github.com/chenzh-lgtm/shuike-manager/settings/secrets/actions
2. 点击 **New repository secret**
3. 逐一添加上述 5 个 Secrets
4. 配置完成后，推送代码到 `main` 分支即可触发 CI + CD

---

## 3. CI 工作流详解

### 触发条件

```yaml
on:
  push:
    branches: [main]       # 推送到 main 分支时
  pull_request:
    branches: [main]       # 创建 PR 到 main 分支时
```

### Job: backend

| 步骤 | 说明 |
|------|------|
| 检出代码 | `actions/checkout@v4` |
| 设置 JDK 8 | Corretto 发行版 |
| 缓存 Maven 依赖 | 基于 `pom.xml` hash，加速后续构建 |
| 启动 MySQL 8.0 服务容器 | `MYSQL_DATABASE=shuike_manager`，健康检查 |
| 初始化数据库 | 执行 `docs/数据库/database.sql` + `prompt_data.sql` |
| 运行测试 | `mvn clean test -B`，42 个用例 |
| 上传测试报告 | `surefire-reports` 作为 artifact 保留 |

### Job: frontend

| 步骤 | 说明 |
|------|------|
| 检出代码 | `actions/checkout@v4` |
| 设置 Node 18 | 缓存 npm 依赖 |
| 编译 | `npm ci && npm run build` |
| 上传产物 | `frontend/dist/` 作为 artifact |

---

## 4. CD 工作流详解

### 触发条件

```yaml
on:
  workflow_dispatch:       # 手动触发（GitHub Actions 页面点按钮）
  push:
    branches: [main]
    paths:                 # 仅以下目录变更时触发
      - 'backend/src/**'
      - 'backend/pom.xml'
      - 'frontend/src/**'
      - 'frontend/package*.json'
```

### Job: deploy

| 步骤 | 说明 |
|------|------|
| 1. 检出代码 | — |
| 2. 设置 JDK 8 + 缓存 | 同 CI |
| 3. 初始化数据库 + 运行测试 | 先测试通过再构建，不通过则中断 |
| 4. 编译后端 JAR | `mvn clean package -DskipTests` |
| 5. 登录 ACR | `docker/login-action` |
| 6. 构建+推送后端镜像 | `docker/build-push-action`（含 GitHub Actions Cache 加速） |
| 7. 构建+推送前端镜像 | 同后端，Node 18 构建 + Nginx 运行 |
| 8. SSH 到 ECS | `appleboy/ssh-action`：拉取镜像 → 标签转换 → `docker compose up` → 验证 |

### ECS 部署脚本

```bash
# 在 ECS 上执行
docker login --username=$ACR_USERNAME --password-stdin $REGISTRY
docker pull $REGISTRY/shuike2026/teacher-file-manager-backend:latest
docker tag  $REGISTRY/shuike2026/teacher-file-manager-backend:latest shuike-backend:1.0
docker pull $REGISTRY/shuike2026/teacher-file-manager-frontend:latest
docker tag  $REGISTRY/shuike2026/teacher-file-manager-frontend:latest shuike-frontend:1.0
cd /opt/shuike && docker compose up -d --no-deps backend frontend
```

---

## 5. 镜像标签策略

所有镜像推送两个标签：

| 标签 | 说明 |
|------|------|
| `1.0` | 固定版本号，不可变 |
| `latest` | 滚动更新，始终指向最新构建 |

ECS 部署使用 `latest` 标签拉取。

> 如需回滚：手动重新构建指定版本的镜像并推送，或使用 ECS 上的本地备份镜像（`docker commit` 保存的快照）。

---

## 6. 缓存策略

| 层 | 方式 | Key |
|------|------|------|
| Maven 依赖 | `actions/cache@v4` | `pom.xml` 的 hash |
| Docker 层 | `docker/build-push-action` 的 `gha` cache | 自动基于 Dockerfile 层 |
| npm 依赖 | `actions/setup-node` 内置缓存 | `package-lock.json` |

---

## 7. 首次配置检查清单

在 ECS 上执行以下命令确保 CD 可以正常部署：

```bash
# 1. 确保 docker-compose.yml 中的 image 名为本地标签
grep "image:" /opt/shuike/docker-compose.yml
# 应输出: shuike-backend:1.0 和 shuike-frontend:1.0

# 2. 确保 docker-compose.yml 中没有 build: 指令（ECS 上不需要编译）
# 如果是本地 docker-compose.yml 同步上去的，需要删除 build: 配置块

# 3. 确保 MinIO bucket 已创建
docker exec shuike-minio mc mb local/shuike-manager 2>/dev/null || echo "已存在"

# 4. 测试手动拉取镜像
docker login crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com
docker pull crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/teacher-file-manager-backend:latest
```

---

> **参考**: [GitHub Actions 文档](https://docs.github.com/actions) · [appleboy/ssh-action](https://github.com/appleboy/ssh-action)
