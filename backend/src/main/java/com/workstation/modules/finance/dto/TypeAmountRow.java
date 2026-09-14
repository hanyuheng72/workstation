package com.workstation.modules.finance.dto;

import java.math.BigDecimal;

/** 按周期分方向汇总的投影，type 为 EXPENSE / INCOME */
public record TypeAmountRow(String label, String type, BigDecimal amount) {
}
