package com.workstation.modules.finance.dto;

import com.workstation.modules.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * occurDate / occurTime 留空表示「现在」。
 */
public record TransactionRequest(
        @NotNull(message = "请选择收入或支出")
        TransactionType type,

        @NotNull(message = "请填写金额")
        @DecimalMin(value = "0.01", message = "金额需大于 0")
        @DecimalMax(value = "10000000.00", message = "金额超出可记录范围")
        BigDecimal amount,

        @NotNull(message = "请选择分类")
        Long categoryId,

        LocalDate occurDate,

        LocalTime occurTime,

        @Size(max = 255, message = "备注最多 255 字")
        String note) {
}
