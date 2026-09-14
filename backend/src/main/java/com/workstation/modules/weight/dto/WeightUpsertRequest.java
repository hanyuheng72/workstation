package com.workstation.modules.weight.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * recordDate 留空表示记在今天。
 * 同一天重复提交会覆盖当天已有记录（一天只保留一条）。
 */
public record WeightUpsertRequest(
        LocalDate recordDate,

        @NotNull(message = "请填写体重")
        @DecimalMin(value = "20.0", message = "体重看起来不太对，请检查")
        @DecimalMax(value = "300.0", message = "体重看起来不太对，请检查")
        BigDecimal weightKg,

        @Size(max = 255, message = "备注最多 255 字")
        String note) {
}
