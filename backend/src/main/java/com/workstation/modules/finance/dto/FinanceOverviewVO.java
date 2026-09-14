package com.workstation.modules.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceOverviewVO(
        LocalDate date,
        BigDecimal todayExpense,
        BigDecimal weekExpense,
        BigDecimal monthIncome,
        BigDecimal monthExpense,
        /** 本月收入 − 本月支出，可为负 */
        BigDecimal monthBalance) {
}
