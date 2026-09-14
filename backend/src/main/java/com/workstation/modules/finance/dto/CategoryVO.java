package com.workstation.modules.finance.dto;

import com.workstation.modules.finance.entity.TransactionType;

public record CategoryVO(
        Long id,
        String code,
        String name,
        TransactionType type,
        String icon,
        Integer sortOrder,
        Boolean isSystem) {
}
