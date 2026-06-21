#!/bin/bash
# ============================================================
# 水课管理系统 - ECS 快速部署脚本
# 版本: v2.3 | 用法: ./deploy-ecs.sh [backend|frontend|all]
# ============================================================
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info(){ echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok(){ echo -e "${GREEN}[OK]${NC}   $1"; }
log_err(){ echo -e "${RED}[ERROR]${NC} $1"; }

# ====== 配置 ======
ECS_IP=${ECS_IP:-47.97.68.38}
ECS_SSH="sshpass -p '118023203czH++' ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -o PubkeyAuthentication=no root@${ECS_IP}"
ECS_SCP="sshpass -p '118023203czH++' scp -o StrictHostKeyChecking=no"
ACR_IMAGE="crpi-x4kb991wgxw0oamg.cn-hangzhou.personal.cr.aliyuncs.com/shuike2026/teacher-file-manager-frontend:1.0"

export JAVA_HOME=${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home}
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

deploy_backend() {
    log_info "=== 部署后端 ==="
    cd "$PROJECT_ROOT/backend"
    mvn clean package -DskipTests -q
    log_ok "JAR 编译完成"

    $ECS_SCP target/teacher-file-manager-1.0.0.jar "root@${ECS_IP}:/opt/shuike/backend/" 2>/dev/null
    log_ok "JAR 上传完成"

    $ECS_SSH '
        docker cp /opt/shuike/backend/teacher-file-manager-1.0.0.jar shuike-backend:/app/app.jar
        docker restart shuike-backend
        echo "等待启动..."
        sleep 18
        docker logs shuike-backend --tail 3
    ' 2>&1 | grep -v "Warning\|Permission denied" | tail -5
    log_ok "后端部署完成"
}

deploy_frontend() {
    log_info "=== 部署前端 ==="
    cd "$PROJECT_ROOT/frontend"
    npx vite build --logLevel warn 2>&1 | tail -1
    docker build --platform linux/amd64 -t shuike-frontend:1.0 . 2>&1 | tail -3
    docker save shuike-frontend:1.0 | gzip > /tmp/shuike-frontend.tar.gz

    $ECS_SCP /tmp/shuike-frontend.tar.gz "root@${ECS_IP}:/opt/shuike/" 2>/dev/null
    log_ok "前端镜像上传完成"

    $ECS_SSH '
        cd /opt/shuike
        gunzip -c shuike-frontend.tar.gz | docker load
        docker tag shuike-frontend:1.0 '$ACR_IMAGE'
        docker compose up -d --no-deps frontend
        sleep 3
        curl -s --max-time 5 -o /dev/null -w "前端HTTP: %{http_code}\n" http://localhost/
    ' 2>&1 | grep -v "Warning\|Permission denied" | tail -5
    log_ok "前端部署完成"
}

db_patches() {
    log_info "=== 数据库补丁 ==="
    $ECS_SSH '
        docker exec shuike-mysql mysql -uroot -p"shuike@2026" shuike_manager -e "
            ALTER TABLE ai_prompt_templates ADD COLUMN material_type VARCHAR(30) DEFAULT NULL AFTER scene;
        " 2>/dev/null || echo "material_type列已存在"
        docker exec shuike-mysql mysql -uroot -p"shuike@2026" shuike_manager -e "
            ALTER TABLE alignment_reports ADD COLUMN report_json LONGTEXT DEFAULT NULL AFTER suggestions;
        " 2>/dev/null || echo "report_json列已存在"
        docker exec shuike-mysql mysql -uroot -p"shuike@2026" shuike_manager -e "
            INSERT INTO system_configs (config_key,config_value,description) VALUES (\"enable_college_review\",\"true\",\"是否启用学院审核\") ON DUPLICATE KEY UPDATE config_value=config_value;
        " 2>/dev/null
        echo "数据库补丁已应用"
    ' 2>&1 | grep -v "Warning\|Permission denied" | grep -v "^$"
    log_ok "数据库补丁完成"
}

case "${1:-all}" in
    backend) deploy_backend ;;
    frontend) deploy_frontend ;;
    db) db_patches ;;
    all)
        deploy_backend
        deploy_frontend
        db_patches
        log_ok "全量部署完成！访问 http://${ECS_IP}"
        ;;
    *) echo "用法: ./deploy-ecs.sh [backend|frontend|db|all]" ;;
esac
