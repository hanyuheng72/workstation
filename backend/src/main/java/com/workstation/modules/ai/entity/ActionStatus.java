package com.workstation.modules.ai.entity;

/**
 * 一条 AI 消息里「待执行动作」的生命周期。
 *
 * 关键约束：模型解析出的动作**绝不直接写业务表**，
 * 必须先落到 PENDING 让用户在界面上确认，确认后才由 ActionExecutor 落库。
 */
public enum ActionStatus {
    /** 这条消息没有夹带动作，纯对话 */
    NONE,
    /** 解析出了动作，等用户确认 */
    PENDING,
    /** 用户点了确认，但还没落库 */
    CONFIRMED,
    /** 用户拒绝 */
    REJECTED,
    /** 已成功落库 */
    EXECUTED,
    /** 落库时失败（例如引用的动作不存在） */
    FAILED
}
