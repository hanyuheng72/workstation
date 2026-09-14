package com.workstation.modules.finance.dto;

import java.math.BigDecimal;

/** 分类占比查询的投影 */
public record CategoryStatRow(Long categoryId, BigDecimal amount) {
}
