package com.workstation.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 每日总结的缓存。仪表盘只读这张表，避免每次刷新页面都打一次 DeepSeek。
 * 用户点「重新生成」才会覆盖。
 */
@Getter
@Setter
@TableName("ai_summary")
public class AiSummary extends BaseEntity {

    private LocalDate summaryDate;

    private String content;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    /** 生成时喂给模型的聚合数据，JSON 字符串，便于回溯「AI 当时看到了什么」 */
    private String dataSnapshot;

    private LocalDateTime generatedAt;
}
