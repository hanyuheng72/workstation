package com.workstation.modules.finance.dto;

import com.workstation.modules.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TransactionVO(
        Long id,
        TransactionType type,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        String categoryIcon,
        LocalDate occurDate,
        LocalTime occurTime,
        String note) {
}
