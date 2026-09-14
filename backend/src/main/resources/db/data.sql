-- ============================================================
-- 个人工作台 Workstation — 字典种子数据
--
-- 只播种「字典表」与用户档案初始行：
--   user_profile / exercise_part / exercise / category
-- 体重、训练、记账、任务等业务表保持为空，
-- 没有真实数据就不画图。
--
-- 本脚本可重复执行（INSERT IGNORE），不会覆盖你已修改的数据。
-- ============================================================

USE `workstation`;

-- ------------------------------------------------------------
-- 用户档案（单行，固定 id = 1）
-- ------------------------------------------------------------
INSERT IGNORE INTO `user_profile` (`id`, `nickname`, `height_cm`, `ai_enabled`, `ai_model`)
VALUES (1, '我', 173.00, 1, 'deepseek-chat');

-- ------------------------------------------------------------
-- 身体部位
-- ------------------------------------------------------------
INSERT IGNORE INTO `exercise_part` (`code`, `name`, `sort_order`) VALUES
    ('CHEST',    '胸',   1),
    ('BACK',     '背',   2),
    ('SHOULDER', '肩',   3),
    ('LEG',      '腿',   4),
    ('ARM',      '手臂', 5),
    ('ABS',      '腹',   6),
    ('CARDIO',   '有氧', 7);

-- ------------------------------------------------------------
-- 训练动作（系统预置 is_default = 1）
-- ------------------------------------------------------------
INSERT IGNORE INTO `exercise` (`part_id`, `name`, `is_default`)
SELECT p.`id`, d.`name`, 1
FROM `exercise_part` p
JOIN (
    SELECT 'CHEST' AS part, '卧推' AS name UNION ALL
    SELECT 'CHEST', '上斜卧推' UNION ALL
    SELECT 'CHEST', '哑铃卧推' UNION ALL
    SELECT 'CHEST', '哑铃飞鸟' UNION ALL
    SELECT 'CHEST', '器械夹胸' UNION ALL
    SELECT 'CHEST', '绳索夹胸' UNION ALL
    SELECT 'CHEST', '双杠臂屈伸' UNION ALL

    SELECT 'BACK', '引体向上' UNION ALL
    SELECT 'BACK', '高位下拉' UNION ALL
    SELECT 'BACK', '杠铃划船' UNION ALL
    SELECT 'BACK', '哑铃划船' UNION ALL
    SELECT 'BACK', '坐姿划船' UNION ALL
    SELECT 'BACK', '硬拉' UNION ALL
    SELECT 'BACK', '直臂下压' UNION ALL

    SELECT 'SHOULDER', '站姿推举' UNION ALL
    SELECT 'SHOULDER', '坐姿哑铃推举' UNION ALL
    SELECT 'SHOULDER', '哑铃侧平举' UNION ALL
    SELECT 'SHOULDER', '哑铃前平举' UNION ALL
    SELECT 'SHOULDER', '反向飞鸟' UNION ALL
    SELECT 'SHOULDER', '面拉' UNION ALL
    SELECT 'SHOULDER', '耸肩' UNION ALL

    SELECT 'LEG', '深蹲' UNION ALL
    SELECT 'LEG', '前蹲' UNION ALL
    SELECT 'LEG', '腿举' UNION ALL
    SELECT 'LEG', '腿屈伸' UNION ALL
    SELECT 'LEG', '腿弯举' UNION ALL
    SELECT 'LEG', '罗马尼亚硬拉' UNION ALL
    SELECT 'LEG', '保加利亚分腿蹲' UNION ALL
    SELECT 'LEG', '提踵' UNION ALL

    SELECT 'ARM', '杠铃弯举' UNION ALL
    SELECT 'ARM', '哑铃弯举' UNION ALL
    SELECT 'ARM', '锤式弯举' UNION ALL
    SELECT 'ARM', '牧师凳弯举' UNION ALL
    SELECT 'ARM', '绳索下压' UNION ALL
    SELECT 'ARM', '窄距卧推' UNION ALL
    SELECT 'ARM', '仰卧臂屈伸' UNION ALL

    SELECT 'ABS', '卷腹' UNION ALL
    SELECT 'ABS', '悬垂举腿' UNION ALL
    SELECT 'ABS', '平板支撑' UNION ALL
    SELECT 'ABS', '俄罗斯转体' UNION ALL
    SELECT 'ABS', '仰卧抬腿' UNION ALL
    SELECT 'ABS', '健腹轮' UNION ALL

    SELECT 'CARDIO', '跑步' UNION ALL
    SELECT 'CARDIO', '快走' UNION ALL
    SELECT 'CARDIO', '骑行' UNION ALL
    SELECT 'CARDIO', '游泳' UNION ALL
    SELECT 'CARDIO', '跳绳' UNION ALL
    SELECT 'CARDIO', '椭圆机' UNION ALL
    SELECT 'CARDIO', '划船机' UNION ALL
    SELECT 'CARDIO', '爬楼梯'
) d ON d.`part` = p.`code`;

-- ------------------------------------------------------------
-- 收支分类（内置 is_system = 1，不允许删除）
-- ------------------------------------------------------------
INSERT IGNORE INTO `category` (`code`, `name`, `type`, `icon`, `sort_order`, `is_system`) VALUES
    ('FOOD',          '餐饮', 'EXPENSE', 'utensils',   1, 1),
    ('TRANSPORT',     '交通', 'EXPENSE', 'bus',        2, 1),
    ('SHOPPING',      '购物', 'EXPENSE', 'shopping',   3, 1),
    ('GAME',          '游戏', 'EXPENSE', 'gamepad',    4, 1),
    ('STUDY',         '学习', 'EXPENSE', 'book',       5, 1),
    ('HOUSING',       '住房', 'EXPENSE', 'home',       6, 1),
    ('OTHER_EXPENSE', '其他', 'EXPENSE', 'dots',       7, 1),
    ('ALLOWANCE',     '生活费', 'INCOME', 'wallet',    1, 1),
    ('SALARY',        '工资',  'INCOME', 'banknote',   2, 1),
    ('OTHER_INCOME',  '其他',  'INCOME', 'dots',       3, 1);
