package com.workstation.modules.focus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 开始一场专注。时长上限 10 小时——再长就该拆成几场，
 * 而且单场过久会让「一次暂停」这条规则变得没有意义。
 */
public record FocusStartRequest(
        @NotBlank(message = "写一下这一场要学什么")
        @Size(max = 100, message = "学习内容最多 100 字")
        String subject,

        @NotNull(message = "先设定一个时长")
        @Min(value = 1, message = "至少 1 分钟")
        @Max(value = 600, message = "单场最长 600 分钟")
        Integer plannedMinutes) {
}
