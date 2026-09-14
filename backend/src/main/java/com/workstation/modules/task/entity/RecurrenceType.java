package com.workstation.modules.task.entity;

/**
 * 第一版只支持 NONE / DAILY / WEEKLY。
 * 加 MONTHLY 是一次 ALTER TABLE ... MODIFY ENUM，扩展成本低。
 */
public enum RecurrenceType {
    NONE,
    DAILY,
    WEEKLY
}
