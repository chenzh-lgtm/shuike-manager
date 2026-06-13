-- ============================================================
-- 水课管理系统 — 数据库初始化脚本
-- 版本: v2.0
-- 日期: 2026-06-12
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `shuike_manager`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `shuike_manager`;

-- ============================================================
-- 第一部分：基础数据表
-- ============================================================

-- 1. 学院表
DROP TABLE IF EXISTS `colleges`;
CREATE TABLE `colleges` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(100) NOT NULL COMMENT '学院名称',
  `code`        VARCHAR(20)  NOT NULL COMMENT '学院代码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '学院描述',
  `sort_order`  INT          DEFAULT 0 COMMENT '排序',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';

-- 2. 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `username`       VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password_hash`  VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name`      VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `email`          VARCHAR(100) DEFAULT NULL,
  `phone`          VARCHAR(20)  DEFAULT NULL,
  `college_id`     BIGINT       DEFAULT NULL COMMENT '所属学院',
  `avatar`         VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `last_login_at`  DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_college_id` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 用户角色关联表
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
  `id`      BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT      NOT NULL COMMENT '用户 ID',
  `role`    VARCHAR(30) NOT NULL COMMENT '角色编码：TEACHER/COLLEGE_REVIEWER/OFFICE/DEAN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4. 学期表
DROP TABLE IF EXISTS `semesters`;
CREATE TABLE `semesters` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(50)  NOT NULL COMMENT '学期名称（如 2025-2026学年第一学期）',
  `code`       VARCHAR(20)  NOT NULL COMMENT '学期代码（如 2025-2026-1）',
  `start_date` DATE         NOT NULL,
  `end_date`   DATE         NOT NULL,
  `is_active`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否当前学期',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

-- 5. 课程表
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `name`            VARCHAR(200) NOT NULL COMMENT '课程名称',
  `code`            VARCHAR(20)  NOT NULL COMMENT '课程代码',
  `college_id`      BIGINT       NOT NULL COMMENT '所属学院',
  `credit_hours`    INT          DEFAULT 0 COMMENT '学分',
  `theory_hours`    INT          DEFAULT 0 COMMENT '理论学时',
  `practice_hours`  INT          DEFAULT 0 COMMENT '实践学时',
  `description`     VARCHAR(500) DEFAULT NULL,
  `status`          TINYINT      DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_college_id` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ============================================================
-- 第二部分：核心业务表
-- ============================================================

-- 6. 授课计划表
DROP TABLE IF EXISTS `teaching_plans`;
CREATE TABLE `teaching_plans` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
  `teacher_id`           BIGINT       NOT NULL COMMENT '教师 ID',
  `course_id`            BIGINT       NOT NULL COMMENT '课程 ID',
  `semester_id`          BIGINT       NOT NULL COMMENT '学期 ID',
  `class_info`           VARCHAR(500) DEFAULT NULL COMMENT '授课班级信息',
  `textbook_info`        VARCHAR(500) DEFAULT NULL COMMENT '教材信息',
  `summary`              TEXT         DEFAULT NULL COMMENT '授课计划摘要（富文本）',
  `status`               VARCHAR(30)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/COLLEGE_PASSED/APPROVED/REJECTED',
  `submit_time`          DATETIME     DEFAULT NULL COMMENT '提交时间',
  `college_review_time`  DATETIME     DEFAULT NULL COMMENT '学院审核时间',
  `office_review_time`   DATETIME     DEFAULT NULL COMMENT '教务处审核时间',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_semester` (`semester_id`),
  KEY `idx_status` (`status`),
  KEY `idx_teacher_semester` (`teacher_id`, `semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授课计划表';

-- 7. 授课计划附件表
DROP TABLE IF EXISTS `teaching_plan_files`;
CREATE TABLE `teaching_plan_files` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_id`    BIGINT       DEFAULT NULL COMMENT "授课计划 ID",
  `file_name`  VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_url`   VARCHAR(500) NOT NULL COMMENT '存储 URL（MinIO 路径）',
  `file_type`  VARCHAR(20)  NOT NULL COMMENT '文件类型：PDF/WORD',
  `file_size`  BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  `sort_order` INT          DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授课计划附件表';

-- 8. 审核记录表
DROP TABLE IF EXISTS `review_records`;
CREATE TABLE `review_records` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_id`      BIGINT       NOT NULL COMMENT '授课计划 ID',
  `reviewer_id`  BIGINT       NOT NULL COMMENT '审核人 ID',
  `review_level` VARCHAR(20)  NOT NULL COMMENT '审核层级：COLLEGE/OFFICE',
  `action`       VARCHAR(20)  NOT NULL COMMENT '操作：APPROVE/REJECT',
  `comment`      TEXT         DEFAULT NULL COMMENT '审核意见',
  `review_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_reviewer` (`reviewer_id`),
  KEY `idx_review_time` (`review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- ============================================================
-- 第三部分：人培方案与课程标准
-- ============================================================

-- 9. 人才培养方案表
DROP TABLE IF EXISTS `talent_cultivation_plans`;
CREATE TABLE `talent_cultivation_plans` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `major_name`     VARCHAR(200) NOT NULL COMMENT '专业名称',
  `major_code`     VARCHAR(20)  DEFAULT NULL COMMENT '专业代码',
  `grade`          VARCHAR(10)  NOT NULL COMMENT '年级（如 2024）',
  `college_id`     BIGINT       NOT NULL COMMENT '所属学院',
  `file_url`       VARCHAR(500) DEFAULT NULL COMMENT '上传文件 URL',
  `content_text`   LONGTEXT     DEFAULT NULL COMMENT '文本内容（附件解析后）',
  `targets`        TEXT         DEFAULT NULL COMMENT '培养目标 JSON',
  `requirements`   TEXT         DEFAULT NULL COMMENT '毕业要求 JSON',
  `course_system`  TEXT         DEFAULT NULL COMMENT '课程体系 JSON',
  `mapping_matrix` TEXT         DEFAULT NULL COMMENT '目标-课程映射矩阵 JSON',
  `dean_id`        BIGINT       NOT NULL COMMENT '创建人（院长）ID',
  `status`         VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_college` (`college_id`),
  KEY `idx_major_grade` (`major_code`, `grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才培养方案表';

-- 10. 课程标准表
DROP TABLE IF EXISTS `course_standards`;
CREATE TABLE `course_standards` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `course_id`        BIGINT       NOT NULL COMMENT '关联课程 ID',
  `tcp_id`           BIGINT       DEFAULT NULL COMMENT '关联人培方案 ID',
  `file_url`         VARCHAR(500) DEFAULT NULL COMMENT '上传文件 URL',
  `content_text`     LONGTEXT     DEFAULT NULL COMMENT '文本内容',
  `knowledge_points` TEXT         DEFAULT NULL COMMENT '知识点 JSON',
  `ability_targets`  TEXT         DEFAULT NULL COMMENT '能力目标 JSON',
  `dean_id`          BIGINT       NOT NULL COMMENT '创建人 ID',
  `status`           VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_tcp_id` (`tcp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程标准表';

-- 11. 对齐分析报告表
DROP TABLE IF EXISTS `alignment_reports`;
CREATE TABLE `alignment_reports` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `tcp_id`            BIGINT       NOT NULL COMMENT '人培方案 ID',
  `major_name`        VARCHAR(200) NOT NULL COMMENT '专业名称',
  `college_id`        BIGINT       NOT NULL COMMENT '所属学院',
  `industry_keywords` TEXT         DEFAULT NULL COMMENT '产业需求关键词 JSON',
  `coverage_score`    INT          DEFAULT NULL COMMENT '覆盖度评分 0-100',
  `gap_analysis`      TEXT         DEFAULT NULL COMMENT '差距分析 JSON',
  `suggestions`       TEXT         DEFAULT NULL COMMENT "改进建议 JSON",
  `report_json`      LONGTEXT     DEFAULT NULL COMMENT "v2.0完整分析报告JSON",
  `dimension_scores`  TEXT         DEFAULT NULL COMMENT '各维度评分 JSON',
  `model_version`     VARCHAR(50)  DEFAULT NULL COMMENT 'AI 模型版本',
  `initiator_id`      BIGINT       NOT NULL COMMENT '发起分析的用户 ID',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tcp_id` (`tcp_id`),
  KEY `idx_college` (`college_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对齐分析报告表';

-- ============================================================
-- 第四部分：AI 评审相关
-- ============================================================

-- 12. 阶段性材料表
DROP TABLE IF EXISTS `phase_materials`;
CREATE TABLE `phase_materials` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `teacher_id`    BIGINT       NOT NULL COMMENT '教师 ID',
  `course_id`     BIGINT       NOT NULL COMMENT '课程 ID',
  `material_type` VARCHAR(30)  NOT NULL COMMENT '材料类型：PROGRESS_TABLE/TEACHING_PLAN/COURSEWARE/EXAM_PLAN/STUDENT_SAMPLE',
  `description`   VARCHAR(500) DEFAULT NULL COMMENT '材料描述',
  `status`        VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/AI_EVALUATING/AI_COMPLETED/AI_CONFIRMED/AI_REJECTED',
  `submit_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段性材料表';

-- 13. 阶段性材料附件表
DROP TABLE IF EXISTS `phase_material_files`;
CREATE TABLE `phase_material_files` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `material_id`  BIGINT       DEFAULT NULL COMMENT "材料 ID",
  `file_name`    VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_url`     VARCHAR(500) NOT NULL COMMENT '存储 URL',
  `file_type`    VARCHAR(20)  NOT NULL COMMENT '文件类型',
  `file_size`    BIGINT       DEFAULT 0 COMMENT '文件大小',
  `text_content` LONGTEXT     DEFAULT NULL COMMENT '文档解析后的文本内容',
  `sort_order`   INT          DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段性材料附件表';

-- 14. AI 评审记录表
DROP TABLE IF EXISTS `ai_evaluations`;
CREATE TABLE `ai_evaluations` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `material_id`      BIGINT       NOT NULL COMMENT '材料 ID',
  `score`            INT          DEFAULT NULL COMMENT 'AI 综合评分 0-100',
  `dimension_scores` TEXT         DEFAULT NULL COMMENT '各维度评分 JSON',
  `suggestions`      TEXT         DEFAULT NULL COMMENT '优化建议 JSON',
  `raw_response`     LONGTEXT     DEFAULT NULL COMMENT 'LLM 原始返回（调试用）',
  `model_version`    VARCHAR(50)  DEFAULT NULL COMMENT 'AI 模型版本',
  `prompt_version`   VARCHAR(20)  DEFAULT NULL COMMENT 'Prompt 模板版本',
  `eval_time`        DATETIME     DEFAULT NULL COMMENT 'AI 评审完成时间',
  `cost_ms`          INT          DEFAULT NULL COMMENT 'AI 调用耗时(毫秒)',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/EVALUATING/COMPLETED/FAILED',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_material_id` (`material_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 评审记录表';

-- 15. 人工复核记录表
DROP TABLE IF EXISTS `manual_reviews`;
CREATE TABLE `manual_reviews` (
  `id`                     BIGINT      NOT NULL AUTO_INCREMENT,
  `evaluation_id`          BIGINT      NOT NULL COMMENT 'AI 评审记录 ID',
  `reviewer_id`            BIGINT      NOT NULL COMMENT '复核人 ID（教务处）',
  `action`                 VARCHAR(20) NOT NULL COMMENT '操作：CONFIRM/REJECT',
  `modified_score`         INT         DEFAULT NULL COMMENT '修改后的评分',
  `modify_reason`          TEXT        DEFAULT NULL COMMENT '修改理由',
  `review_comment`         TEXT        DEFAULT NULL COMMENT '复核意见',
  `revision_requirements`  TEXT        DEFAULT NULL COMMENT '修改要求（驳回时）',
  `deadline`               DATE        DEFAULT NULL COMMENT '修改截止日期',
  `review_time`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evaluation_id` (`evaluation_id`),
  KEY `idx_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工复核记录表';

-- 16. AI Prompt 模板表
DROP TABLE IF EXISTS `ai_prompt_templates`;
CREATE TABLE `ai_prompt_templates` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `scene`         VARCHAR(50)  NOT NULL COMMENT "场景：MATERIAL_EVALUATION/INDUSTRY_ANALYSIS/ALIGNMENT_ANALYSIS",
  `material_type` VARCHAR(30)  DEFAULT NULL COMMENT "材料类型：TEACHING_PLAN/LESSON_PLAN/COURSEWARE/EXAM_PLAN",
  `dimension`     VARCHAR(50)  DEFAULT NULL COMMENT '评分维度（仅评审场景）',
  `template_text` LONGTEXT     NOT NULL COMMENT 'Prompt 模板内容',
  `version`       VARCHAR(20)  NOT NULL COMMENT '版本号',
  `is_active`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否当前激活版本',
  `description`   VARCHAR(500) DEFAULT NULL,
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人 ID',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_active` (`scene`, `is_active`),
  KEY `idx_version` (`scene`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Prompt 模板表';

-- ============================================================
-- 第五部分：系统辅助表
-- ============================================================

-- 17. 通知表
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL COMMENT '接收用户 ID',
  `type`       VARCHAR(30)  NOT NULL COMMENT '通知类型：REVIEW_RESULT/AI_REVIEW_COMPLETED/AI_MANUAL_REVIEW/SYSTEM',
  `title`      VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content`    TEXT         DEFAULT NULL COMMENT '通知内容',
  `target_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  `is_read`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at`    DATETIME     DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_unread` (`user_id`, `is_read`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 18. 操作日志表
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作人 ID',
  `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作人用户名',
  `module`      VARCHAR(50)  NOT NULL COMMENT '操作模块',
  `action`      VARCHAR(50)  NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/APPROVE/REJECT/EXPORT',
  `target_type` VARCHAR(50)  DEFAULT NULL COMMENT '目标类型',
  `target_id`   BIGINT       DEFAULT NULL COMMENT '目标 ID',
  `detail`      TEXT         DEFAULT NULL COMMENT '操作详情 JSON',
  `ip_address`  VARCHAR(50)  DEFAULT NULL,
  `user_agent`  VARCHAR(500) DEFAULT NULL,
  `cost_ms`     INT          DEFAULT NULL COMMENT '耗时(毫秒)',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 19. 系统配置表
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT         DEFAULT NULL COMMENT '配置值',
  `description`  VARCHAR(500) DEFAULT NULL,
  `updated_by`   BIGINT       DEFAULT NULL,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================================
-- 第六部分：初始化数据
-- ============================================================

-- 学院初始化数据
INSERT INTO `colleges` (`name`, `code`, `description`, `sort_order`) VALUES
('数学学院',   'MATH',   '数学与应用数学、信息与计算科学等专业', 1),
('计算机学院', 'CS',     '计算机科学与技术、软件工程等专业',     2),
('物理学院',   'PHYS',   '物理学、应用物理学等专业',             3),
('教务处',     'OFFICE', '教务处（非教学单位）',                  99);

-- 学期初始化数据
INSERT INTO `semesters` (`name`, `code`, `start_date`, `end_date`, `is_active`) VALUES
('2025-2026学年第一学期', '2025-2026-1', '2025-09-01', '2026-01-15', 1),
('2025-2026学年第二学期', '2025-2026-2', '2026-02-16', '2026-07-10', 0);

-- 用户初始化数据（密码均为 123456 的 BCrypt 哈希值）
-- 实际部署时请替换为安全的 BCrypt 哈希
INSERT INTO `users` (`username`, `password_hash`, `real_name`, `email`, `college_id`, `status`) VALUES
('admin',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@school.edu.cn',    4, 1),
('teacher1',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李老师',     'teacher1@school.edu.cn',  1, 1),
('teacher2',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王老师',     'teacher2@school.edu.cn',  1, 1),
('teacher3',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张老师',     'teacher3@school.edu.cn',  2, 1),
('reviewer1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵主任',     'reviewer1@school.edu.cn', 1, 1),
('reviewer2',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '钱主任',     'reviewer2@school.edu.cn', 2, 1),
('office1',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙教务',     'office1@school.edu.cn',   4, 1),
('dean1',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周院长',     'dean1@school.edu.cn',     1, 1),
('dean2',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴院长',     'dean2@school.edu.cn',     2, 1);

-- 用户角色初始化数据
INSERT INTO `user_roles` (`user_id`, `role`) VALUES
(1, 'OFFICE'),              -- admin = 教务处
(2, 'TEACHER'),             -- teacher1 = 教师
(3, 'TEACHER'),             -- teacher2 = 教师
(4, 'TEACHER'),             -- teacher3 = 教师
(5, 'COLLEGE_REVIEWER'),    -- reviewer1 = 学院审核员
(6, 'COLLEGE_REVIEWER'),    -- reviewer2 = 学院审核员
(7, 'OFFICE'),              -- office1 = 教务处
(8, 'DEAN'),                -- dean1 = 院长
(9, 'DEAN');                -- dean2 = 院长

-- 课程初始化数据
INSERT INTO `courses` (`name`, `code`, `college_id`, `credit_hours`, `theory_hours`, `practice_hours`, `description`) VALUES
('高等数学(上)', 'MATH101', 1, 5, 64, 16, '函数、极限、导数、积分等基础内容'),
('高等数学(下)', 'MATH102', 1, 5, 64, 16, '多元微积分、级数、微分方程'),
('线性代数',     'MATH201', 1, 3, 48, 0,  '矩阵、向量空间、线性变换'),
('概率论与数理统计', 'MATH301', 1, 4, 48, 16, '概率基础、统计推断、回归分析'),
('程序设计基础', 'CS101',   2, 4, 32, 32, 'C语言程序设计入门'),
('数据结构',     'CS201',   2, 4, 48, 16, '常用数据结构与算法');

-- 系统配置初始化数据
INSERT INTO `system_configs` (`config_key`, `config_value`, `description`) VALUES
('enable_captcha',           'false',  '是否启用登录验证码'),
('max_login_attempts',       '5',      '最大登录失败次数'),
('lock_duration_minutes',    '15',     '账户锁定时间(分钟)'),
('max_file_size_mb',         '20',     '单文件最大上传大小(MB)'),
('max_file_count',           '10',     '单次最大上传文件数'),
('ai_temperature',           '0.3',    'AI 模型温度参数'),
('ai_default_model',         'doubao-pro-32k', '默认 AI 模型名称'),
('notification_poll_seconds','30',     '通知轮询间隔(秒)'),
('jwt_access_expire_seconds','7200',   'Access Token 过期时间(秒)'),
('jwt_refresh_expire_seconds','604800','Refresh Token 过期时间(秒)');

-- AI Prompt 模板初始化数据
INSERT INTO `ai_prompt_templates` (`scene`, `dimension`, `template_text`, `version`, `is_active`, `description`) VALUES
('MATERIAL_EVALUATION', 'completeness', '你是一位资深教学评审专家。请对以下{{materialType}}进行「内容完整性」维度的评审，评分范围0-100分。\n\n评审标准：\n1. 是否涵盖课程核心知识点\n2. 教学目标是否明确\n3. 教学环节是否完整（导入、讲解、练习、总结）\n4. 考核方式是否合理\n\n请以严格的JSON格式返回结果：\n{"score": 85, "issues": ["问题1", "问题2"], "suggestion": "综合建议"}', 'v1.0', 1, '教学材料评审-内容完整性维度'),

('MATERIAL_EVALUATION', 'standard_match', '你是一位资深教学评审专家。请对以下{{materialType}}进行「与课程标准匹配度」维度的评审，评分范围0-100分。\n\n评审标准：\n1. 教学内容是否与课程标准要求一致\n2. 知识点覆盖是否全面\n3. 能力目标是否对应课程标准\n4. 学时分配是否合理\n\n请以严格的JSON格式返回结果：\n{"score": 82, "issues": ["问题1"], "suggestion": "综合建议"}', 'v1.0', 1, '教学材料评审-课程标准匹配度维度'),

('MATERIAL_EVALUATION', 'format', '你是一位资深教学评审专家。请对以下{{materialType}}进行「格式规范性」维度的评审，评分范围0-100分。\n\n评审标准：\n1. 文档排版是否规范\n2. 引用是否标准\n3. 附件是否完整\n4. 表格/图表是否清晰\n\n请以严格的JSON格式返回结果：\n{"score": 88, "issues": [], "suggestion": "综合建议"}', 'v1.0', 1, '教学材料评审-格式规范性维度'),

('MATERIAL_EVALUATION', 'innovation', '你是一位资深教学评审专家。请对以下{{materialType}}进行「创新性」维度的评审，评分范围0-100分。\n\n评审标准：\n1. 教学方法是否有创新\n2. 案例是否新颖且贴近实际\n3. 实践环节设计是否有特色\n4. 是否融入新技术/新理念\n\n请以严格的JSON格式返回结果：\n{"score": 80, "issues": ["问题1"], "suggestion": "综合建议"}', 'v1.0', 1, '教学材料评审-创新性维度'),

('INDUSTRY_ANALYSIS', NULL, '你是一位产业需求分析专家。请基于以下专业人才培养方案，分析当前产业对该专业的人才需求趋势。\n\n专业名称：{{majorName}}\n人培方案内容：\n{{tcpContent}}\n\n课程体系：\n{{courseSystem}}\n\n请以严格的JSON格式返回：\n{\n  "industryKeywords": ["关键词1","关键词2",...],  // Top10\n  "coverageScore": 78,  // 0-100\n  "gapAnalysis": [{"capability":"能力项","coverage":"NONE/WEAK/STRONG","suggestion":"建议"}],\n  "suggestions": ["建议1","建议2"]\n}', 'v1.0', 1, '产业需求分析'),

('ALIGNMENT_ANALYSIS', NULL, '你是一位教育教学评估专家。请对以下人才培养方案与课程体系进行对齐分析。\n\n人培方案培养目标：\n{{targets}}\n\n毕业要求：\n{{requirements}}\n\n课程体系：\n{{courseSystem}}\n\n请以严格的JSON格式返回：\n{\n  "mappingCoverage": {"目标1":["课程A","课程B"], "目标2":["课程C"]},\n  "weakPoints": [{"target":"目标X","reason":"缺少对应课程支撑","suggestion":"建议新增/调整课程"}],\n  "overallScore": 80,  // 0-100\n  "suggestions": ["建议1","建议2"]\n}', 'v1.0', 1, '人培方案对齐分析');

-- ============================================================
-- 第七部分：外键约束（可选，按需启用）
-- ============================================================

-- 注意：外键约束可能影响写入性能和数据清理灵活性。
-- 建议在应用层保证数据一致性，生产环境可按需启用。

-- ALTER TABLE `users` ADD CONSTRAINT `fk_users_college` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`) ON DELETE SET NULL;
-- ALTER TABLE `user_roles` ADD CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `teaching_plans` ADD CONSTRAINT `fk_plans_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`);
-- ALTER TABLE `teaching_plans` ADD CONSTRAINT `fk_plans_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`);
-- ALTER TABLE `teaching_plans` ADD CONSTRAINT `fk_plans_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`);
-- ALTER TABLE `teaching_plan_files` ADD CONSTRAINT `fk_plan_files_plan` FOREIGN KEY (`plan_id`) REFERENCES `teaching_plans` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `review_records` ADD CONSTRAINT `fk_review_plan` FOREIGN KEY (`plan_id`) REFERENCES `teaching_plans` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `review_records` ADD CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`);
-- ALTER TABLE `phase_materials` ADD CONSTRAINT `fk_phase_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`);
-- ALTER TABLE `phase_materials` ADD CONSTRAINT `fk_phase_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`);
-- ALTER TABLE `phase_material_files` ADD CONSTRAINT `fk_phase_files_material` FOREIGN KEY (`material_id`) REFERENCES `phase_materials` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `ai_evaluations` ADD CONSTRAINT `fk_ai_eval_material` FOREIGN KEY (`material_id`) REFERENCES `phase_materials` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `manual_reviews` ADD CONSTRAINT `fk_manual_eval` FOREIGN KEY (`evaluation_id`) REFERENCES `ai_evaluations` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `manual_reviews` ADD CONSTRAINT `fk_manual_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`);

-- ============================================================
-- 初始化完成
-- ============================================================
