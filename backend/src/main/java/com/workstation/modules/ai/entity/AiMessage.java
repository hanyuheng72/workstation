package com.workstation.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    private Long conversationId;

    private AiRole role;

    private String content;

    /** 模型判定的意图，如 LOG_WEIGHT / LOG_WORKOUT */
    private String intent;

    /** 解析出的草稿内容，JSON 字符串 */
    private String parsedPayload;

    private ActionStatus actionStatus;

    /** 落库后指向的业务实体，便于回溯这条 AI 消息改了什么 */
    private String relatedEntityType;

    private Long relatedEntityId;

    private Integer promptTokens;

    private Integer completionTokens;
}
