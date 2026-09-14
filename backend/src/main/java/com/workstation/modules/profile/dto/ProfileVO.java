package com.workstation.modules.profile.dto;

import java.math.BigDecimal;

public record ProfileVO(
        Long id,
        String nickname,
        BigDecimal heightCm,
        Boolean aiEnabled,
        String aiModel) {
}
