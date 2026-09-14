package com.workstation.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    private String title;
}
