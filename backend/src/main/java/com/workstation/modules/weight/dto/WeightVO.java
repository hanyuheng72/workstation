package com.workstation.modules.weight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightVO(
        Long id,
        LocalDate recordDate,
        BigDecimal weightKg,
        String note) {
}
