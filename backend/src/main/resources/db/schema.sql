-- ============================================================
-- 个人工作台 Workstation — 数据库结构
-- MySQL 8.0+ / utf8mb4 / InnoDB
--
-- 前置：workstation 库必须已经存在。
--   本机：先跑 00-create-database.sql
--   云上：在服务商控制台里建好库，再用 -D workstation 连过来跑本脚本
--
-- 共 14 张表。本脚本可重复执行（CREATE TABLE IF NOT EXISTS），
-- 已有表不会被改动也不会丢数据，所以升级版本时直接重跑本脚本即可补上新表。
-- ============================================================

USE `workstation`;

-- ------------------------------------------------------------
-- 1. user_profile — 单用户档案（单行表）
--    刻意不设 target_weight_kg：当前体重恒取 weight_record 最新一条
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '我',
    `height_cm`   DECIMAL(5,2) NOT NULL DEFAULT 173.00 COMMENT 'BMI 计算依据',
    `ai_enabled`  TINYINT(1)   NOT NULL DEFAULT 1,
    `ai_model`    VARCHAR(50)  NOT NULL DEFAULT 'deepseek-chat',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户档案（单行）';

-- ------------------------------------------------------------
-- 2. weight_record — 体重（一天一条）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `weight_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `record_date` DATE         NOT NULL COMMENT '业务日期，一天一条',
    `weight_kg`   DECIMAL(5,2) NOT NULL,
    `note`        VARCHAR(255) DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_date` (`record_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '体重记录';

-- ------------------------------------------------------------
-- 3. exercise_part — 身体部位字典
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exercise_part` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `code`       VARCHAR(20) NOT NULL COMMENT 'CARDIO 表示有氧，走时长/距离而非重量x次数',
    `name`       VARCHAR(20) NOT NULL,
    `sort_order` INT         NOT NULL DEFAULT 0,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '身体部位字典';

-- ------------------------------------------------------------
-- 4. exercise — 训练动作字典
--    deleted 为逻辑删除位；重新创建同名动作时走「恢复」而非插入，
--    以保留历史训练记录的关联。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exercise` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `part_id`    BIGINT      NOT NULL,
    `name`       VARCHAR(50) NOT NULL,
    `is_default` TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1=系统预置',
    `deleted`    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除位',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_part_name` (`part_id`, `name`),
    CONSTRAINT `fk_exercise_part` FOREIGN KEY (`part_id`)
        REFERENCES `exercise_part` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '训练动作字典';

-- ------------------------------------------------------------
-- 5. workout_record — 一次训练（某天 + 某部位 + 某动作 = 一条）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `workout_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `record_date` DATE         NOT NULL,
    `part_id`     BIGINT       NOT NULL,
    `exercise_id` BIGINT       NOT NULL,
    `note`        VARCHAR(255) DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_date` (`record_date`),
    KEY `idx_exercise_date` (`exercise_id`, `record_date` DESC),
    KEY `idx_part` (`part_id`),
    CONSTRAINT `fk_workout_part` FOREIGN KEY (`part_id`)
        REFERENCES `exercise_part` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_workout_exercise` FOREIGN KEY (`exercise_id`)
        REFERENCES `exercise` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '训练记录（按动作聚合）';

-- ------------------------------------------------------------
-- 6. workout_set — 组
--    力量组：weight_kg + reps
--    有氧组：duration_min 或 distance_km
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `workout_set` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `record_id`    BIGINT       NOT NULL,
    `set_index`    INT          NOT NULL DEFAULT 1 COMMENT '第几组，从 1 开始',
    `weight_kg`    DECIMAL(6,2) DEFAULT NULL,
    `reps`         INT          DEFAULT NULL,
    `duration_min` DECIMAL(6,1) DEFAULT NULL,
    `distance_km`  DECIMAL(6,2) DEFAULT NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    CONSTRAINT `fk_set_record` FOREIGN KEY (`record_id`)
        REFERENCES `workout_record` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '训练组';

-- ------------------------------------------------------------
-- 7. category — 收支分类字典
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `code`       VARCHAR(30) NOT NULL,
    `name`       VARCHAR(20) NOT NULL,
    `type`       ENUM('EXPENSE','INCOME') NOT NULL,
    `icon`       VARCHAR(30) DEFAULT NULL COMMENT '前端图标标识',
    `sort_order` INT         NOT NULL DEFAULT 0,
    `is_system`  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1=内置分类，不允许删除',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type_sort` (`type`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收支分类字典';

-- ------------------------------------------------------------
-- 8. transaction_record — 记账流水
--    表名避开 SQL 保留字 transaction
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_record` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `type`        ENUM('EXPENSE','INCOME') NOT NULL,
    `amount`      DECIMAL(12,2) NOT NULL,
    `category_id` BIGINT        NOT NULL,
    `occur_date`  DATE          NOT NULL,
    `occur_time`  TIME          DEFAULT NULL,
    `note`        VARCHAR(255)  DEFAULT NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_occur_date` (`occur_date`),
    KEY `idx_type_date` (`type`, `occur_date`),
    KEY `idx_category` (`category_id`),
    CONSTRAINT `fk_txn_category` FOREIGN KEY (`category_id`)
        REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '记账流水';

-- ------------------------------------------------------------
-- 9. task — 任务定义
--    task_type 只有 TODAY / LONG_TERM。
--    「每周重复」是 recurrence_type 的事，与任务分类无关。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `task` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `title`               VARCHAR(100) NOT NULL,
    `description`         VARCHAR(500) DEFAULT NULL,
    `task_type`           ENUM('TODAY','LONG_TERM') NOT NULL DEFAULT 'TODAY',
    `priority`            TINYINT      NOT NULL DEFAULT 0 COMMENT '越大越优先',
    `plan_date`           DATE         DEFAULT NULL COMMENT 'TODAY 任务的目标日期；重复任务的重复起始日',
    `due_date`            DATE         DEFAULT NULL COMMENT 'LONG_TERM 的截止日',
    `recurrence_type`     ENUM('NONE','DAILY','WEEKLY') NOT NULL DEFAULT 'NONE',
    `recurrence_interval` INT          NOT NULL DEFAULT 1,
    `recurrence_end_date` DATE         DEFAULT NULL,
    `status`              ENUM('PENDING','DONE','CANCELLED') NOT NULL DEFAULT 'PENDING'
                          COMMENT '非重复任务与长期任务的状态；重复任务看 task_occurrence',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`task_type`, `status`),
    KEY `idx_plan_date` (`plan_date`),
    KEY `idx_due_date` (`due_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务定义';

-- ------------------------------------------------------------
-- 10. task_occurrence — 任务在某天的状态
--     统一模型：非重复任务在 plan_date 上恰好一条；
--     重复任务按查询区间幂等补齐（INSERT IGNORE 靠唯一键去重）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `task_occurrence` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `task_id`      BIGINT   NOT NULL,
    `occur_date`   DATE     NOT NULL,
    `status`       ENUM('PENDING','DONE','CANCELLED') NOT NULL DEFAULT 'PENDING',
    `completed_at` DATETIME DEFAULT NULL,
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_date` (`task_id`, `occur_date`),
    KEY `idx_occur_date` (`occur_date`),
    CONSTRAINT `fk_occurrence_task` FOREIGN KEY (`task_id`)
        REFERENCES `task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务实例（按天）';

-- ------------------------------------------------------------
-- 11. ai_summary — AI 今日总结缓存（仪表盘只读它，避免重复计费）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_summary` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT,
    `summary_date`      DATE        NOT NULL,
    `content`           TEXT        NOT NULL,
    `model`             VARCHAR(50) DEFAULT NULL,
    `prompt_tokens`     INT         DEFAULT NULL,
    `completion_tokens` INT         DEFAULT NULL,
    `data_snapshot`     JSON        DEFAULT NULL COMMENT '生成时喂给模型的聚合数据，便于回溯',
    `generated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_summary_date` (`summary_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 每日总结缓存';

-- ------------------------------------------------------------
-- 12. ai_conversation — AI 会话
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(100) NOT NULL DEFAULT '新对话',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 会话';

-- ------------------------------------------------------------
-- 13. ai_memory — AI 对我的画像记忆
--     与 ai_message 的区别：消息是每天的对话流水，记忆是长期沉淀下来的、
--     关于「我是谁」的事实与偏好。只有用户点过确认的才会写进来。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_memory` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `content`    VARCHAR(500) NOT NULL COMMENT '一句话陈述，如「身高173cm，目标是增肌」',
    `category`   VARCHAR(30)  NOT NULL DEFAULT 'OTHER'
                 COMMENT 'PROFILE 基本资料 / GOAL 目标 / PREFERENCE 偏好 / HABIT 习惯 / OTHER',
    `source`     VARCHAR(20)  NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT 对话中提炼 / MANUAL 手动添加',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 长期记忆（画像）';

-- ------------------------------------------------------------
-- 14. ai_message — AI 消息（含意图草稿与确认状态）
--     action_status 保证「解析 → 草稿 → 用户确认 → 落库」，
--     模型永远不直接改业务表。记忆的提议也走这条链路
--     （intent = SAVE_MEMORY），所以不需要另造一套确认机制。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `conversation_id`     BIGINT      NOT NULL,
    `role`                ENUM('USER','ASSISTANT','SYSTEM') NOT NULL,
    `content`             TEXT        NOT NULL,
    `intent`              VARCHAR(50) DEFAULT NULL COMMENT 'LOG_WEIGHT / LOG_WORKOUT / LOG_EXPENSE ...',
    `parsed_payload`      JSON        DEFAULT NULL COMMENT '解析出的草稿内容',
    `action_status`       ENUM('NONE','PENDING','CONFIRMED','REJECTED','EXECUTED','FAILED') NOT NULL DEFAULT 'NONE',
    `related_entity_type` VARCHAR(30) DEFAULT NULL,
    `related_entity_id`   BIGINT      DEFAULT NULL,
    `prompt_tokens`       INT         DEFAULT NULL,
    `completion_tokens`   INT         DEFAULT NULL,
    `created_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`conversation_id`, `created_at`),
    KEY `idx_action_status` (`action_status`),
    CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`)
        REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 消息';
