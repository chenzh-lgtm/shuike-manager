#!/bin/bash
# ============================================================
# 水课管理系统 - 一键启动脚本
# 版本: v2.3 | 日期: 2026-06-21
# 用法: ./start.sh [build|up|down|restart|logs|status|init]
# ============================================================
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# JAVA_HOME（macOS 默认 JDK 8 路径）
export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home}

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
step()      { echo -e "\n${GREEN}━━━ $1 ━━━${NC}"; }

# ═══════════════════════════════════════════════════════
# 构建后端 JAR
# ═══════════════════════════════════════════════════════
build_backend() {
    step "1/4 编译后端"
    cd "$PROJECT_ROOT/backend"
    log_info "Maven 编译中 (Java 8)..."
    mvn clean package -DskipTests -q
    log_ok "后端编译完成: $(ls -lh target/teacher-file-manager-1.0.0.jar | awk '{print $5}')"
    cd "$PROJECT_ROOT"
}

# ═══════════════════════════════════════════════════════
# 构建前端
# ═══════════════════════════════════════════════════════
build_frontend() {
    step "2/4 构建前端"
    cd "$PROJECT_ROOT/frontend"
    log_info "Vite 编译中..."
    npx vite build --logLevel warn 2>&1 | tail -1
    log_ok "前端编译完成"
    cd "$PROJECT_ROOT"
}

# ═══════════════════════════════════════════════════════
# 启动 Docker 服务
# ═══════════════════════════════════════════════════════
start_services() {
    step "3/4 启动 Docker 服务"
    log_info "拉取镜像并构建..."
    docker compose up -d --build 2>&1 | grep -E "Started|Running|Built" || true
    log_ok "Docker 服务已启动"
}

# ═══════════════════════════════════════════════════════
# 初始化（MinIO桶 + Prompt模板）
# ═══════════════════════════════════════════════════════
init_services() {
    step "4/4 初始化服务"

    # 等待 MySQL 就绪
    log_info "等待 MySQL 就绪..."
    for i in $(seq 1 30); do
        if docker exec shuike-mysql mysqladmin ping -h localhost -u root -pshuike@2026 --silent 2>/dev/null; then
            log_ok "MySQL 已就绪"
            break
        fi
        sleep 2
    done

    # 等待后端就绪
    log_info "等待后端启动..."
    for i in $(seq 1 30); do
        if curl -s --max-time 2 http://localhost:8080/api/auth/login -o /dev/null 2>/dev/null; then
            log_ok "后端已就绪"
            break
        fi
        sleep 2
    done

    # 创建 MinIO 存储桶
    if docker exec shuike-minio mc ls local/shuike-manager &>/dev/null; then
        log_ok "MinIO 存储桶已存在"
    else
        log_info "创建 MinIO 存储桶..."
        docker exec shuike-minio mc mb local/shuike-manager 2>/dev/null || true
        log_ok "MinIO 存储桶已创建"
    fi

    # 导入 Prompt 模板数据
    log_info "导入 Prompt 模板..."
    if docker exec shuike-mysql mysql -u root -pshuike@2026 shuike_manager -e "SELECT COUNT(*) FROM ai_prompt_templates WHERE version='v2.0'" 2>/dev/null | grep -q 16; then
        log_ok "Prompt 模板已存在 (16条 v2.0)"
    else
        docker exec -i shuike-mysql mysql -u root -pshuike@2026 --default-character-set=utf8mb4 shuike_manager < "$PROJECT_ROOT/docs/数据库/prompt_data.sql" 2>/dev/null
        log_ok "Prompt 模板已导入"
    fi

    # 验证系统配置
    CFG_COUNT=$(docker exec shuike-mysql mysql -u root -pshuike@2026 -N -e "SELECT COUNT(*) FROM shuike_manager.system_configs" 2>/dev/null || echo 0)
    log_ok "系统配置: ${CFG_COUNT} 项"
}

# ═══════════════════════════════════════════════════════
# 显示状态
# ═══════════════════════════════════════════════════════
show_status() {
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║        水课管理系统 v2.3 启动完成                    ║${NC}"
    echo -e "${GREEN}╠══════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}║${NC}  前端页面:    ${BLUE}http://localhost${NC}"
    echo -e "${GREEN}║${NC}  后端 API:    ${BLUE}http://localhost:8080${NC}"
    echo -e "${GREEN}║${NC}  MinIO 控制台: ${BLUE}http://localhost:9001${NC} (minioadmin/minioadmin123)"
    echo -e "${GREEN}║${NC}  MySQL:       ${BLUE}localhost:3307${NC} (root/shuike@2026)"
    echo -e "${GREEN}║${NC}  Redis:       ${BLUE}localhost:6379${NC}"
    echo -e "${GREEN}╠══════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}║${NC}  默认账号:    admin / 123456 (教务处)           ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  文档:        ${BLUE}docs/测试报告/PRD验收测试报告-v2.3.md${GREEN}   ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# ═══════════════════════════════════════════════════════
# 主入口
# ═══════════════════════════════════════════════════════
case "${1:-start}" in
    start|up)
        log_info "一键启动水课管理系统 v2.3..."
        build_backend
        build_frontend
        start_services
        init_services
        show_status
        log_ok "启动完成!"
        ;;
    build)
        build_backend
        build_frontend
        log_ok "构建完成，使用 './start.sh up' 启动服务"
        ;;
    up)
        start_services
        init_services
        show_status
        ;;
    down|stop)
        log_info "停止所有服务..."
        docker compose down
        log_ok "服务已停止"
        ;;
    restart)
        log_info "重启所有服务..."
        docker compose restart
        sleep 10
        log_ok "服务已重启"
        show_status
        ;;
    logs)
        docker compose logs -f --tail=50
        ;;
    status)
        echo ""
        docker ps --filter "name=shuike" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        echo ""
        ;;
    init)
        init_services
        show_status
        ;;
    deploy)
        # ECS 快速部署
        if [ -z "${ECS_IP:-}" ]; then
            log_error "请设置环境变量: export ECS_IP=你的ECS地址"
            exit 1
        fi
        log_info "部署到 ECS: $ECS_IP"
        build_backend
        log_info "上传 JAR..."
        scp "$PROJECT_ROOT/backend/target/teacher-file-manager-1.0.0.jar" "root@${ECS_IP}:/opt/shuike/backend/"
        ssh "root@${ECS_IP}" '
            docker cp /opt/shuike/backend/teacher-file-manager-1.0.0.jar shuike-backend:/app/app.jar
            docker restart shuike-backend
            sleep 15
            docker logs shuike-backend --tail 5
        '
        log_ok "ECS 部署完成"
        ;;
    *)
        echo "用法: ./start.sh [start|build|up|down|restart|logs|status|init|deploy]"
        echo ""
        echo "  start    一键构建+启动全部服务 (默认)"
        echo "  build    仅构建前后端代码"
        echo "  up       仅启动 Docker (已构建)"
        echo "  down     停止所有服务"
        echo "  restart  重启所有服务"
        echo "  logs     查看实时日志"
        echo "  status   查看服务运行状态"
        echo "  init     仅初始化 (Prompt模板/MinIO桶)"
        echo "  deploy   部署到 ECS (需设 ECS_IP 环境变量)"
        ;;
esac
