package com.workstation.modules.weight.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * BMI 展示数据。
 * scaleMin / scaleMax 是刻度尺的取值跨度，bands 是尺上的分区，
 * bmi 落在哪里由前端按 (bmi - scaleMin) / (scaleMax - scaleMin) 定位。
 * 没有任何体重记录时 bmi 为 null。
 */
public record BmiVO(
        BigDecimal bmi,
        String category,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal scaleMin,
        BigDecimal scaleMax,
        /** 身高对应的健康体重区间，给用户一个具体的目标参考 */
        BigDecimal healthyMinKg,
        BigDecimal healthyMaxKg,
        List<BmiBandVO> bands) {

    public static BmiVO empty(BigDecimal heightCm) {
        return new BmiVO(null, null, heightCm, null, null, null, null, null, List.of());
    }
}
