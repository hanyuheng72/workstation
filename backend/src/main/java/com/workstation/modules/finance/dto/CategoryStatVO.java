package com.workstation.modules.finance.dto;

import java.math.BigDecimal;

/**
 * 分类占比。
 * code 一并返回，好让前端按分类身份固定配色——
 * 若前端按金额排名配色，同一个月度之间颜色会互相窜位。
 */
public record CategoryStatVO(
        Long categoryId,
        String code,
        String categoryName,
        String icon,
        BigDecimal amount,
        BigDecimal percent) {
}
