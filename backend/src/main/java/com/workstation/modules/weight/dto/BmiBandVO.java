package com.workstation.modules.weight.dto;

import java.math.BigDecimal;

/** 刻度尺上的一段区间，供前端自绘 BMI 直线图 */
public record BmiBandVO(String code, String label, BigDecimal min, BigDecimal max) {
}
