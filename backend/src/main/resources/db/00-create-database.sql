-- ============================================================
-- 个人工作台 —— 创建数据库
--
-- 只在「你有权建库」的环境里执行（比如本机 MySQL）。
--
-- 云数据库通常不给应用账号建库权限，那就在服务商控制台里手动建
-- （Aiven: 服务页 → Databases → Create database，名字填 workstation），
-- 然后直接跑 schema.sql 即可，跳过这个文件。
--
-- 本脚本可重复执行，不会影响其它数据库。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `workstation`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
