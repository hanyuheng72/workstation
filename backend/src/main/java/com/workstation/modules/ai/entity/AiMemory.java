package com.workstation.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 对我的画像记忆。
 *
 * 与 {@link AiMessage} 的分工：消息是每天的对话流水，会一直增长；
 * 记忆是长期沉淀下来的、关于「我是谁」的事实与偏好，条数很少。
 * 只有用户在界面上点过确认的才会写进来——模型不能自己往里塞。
 */
@Getter
@Setter
@TableName("ai_memory")
public class AiMemory extends BaseEntity {

    private String content;

    /** PROFILE 基本资料 / GOAL 目标 / PREFERENCE 偏好 / HABIT 习惯 / OTHER */
    private String category;

    /** CHAT 对话中提炼 / MANUAL 手动添加 */
    private String source;
}
