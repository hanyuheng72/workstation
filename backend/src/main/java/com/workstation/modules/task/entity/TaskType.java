package com.workstation.modules.task.entity;

/**
 * 任务分类。
 *
 * 原先还有 WEEKLY（周任务），实测用不上，已移除——
 * 「每周重复」是另一回事，看 {@link RecurrenceType}。
 */
public enum TaskType {
    /** 某一天要做的事，落在那天的今日任务里 */
    TODAY,
    /** 长期目标，只出现在长期列表，不占日历也不进今日任务 */
    LONG_TERM
}
