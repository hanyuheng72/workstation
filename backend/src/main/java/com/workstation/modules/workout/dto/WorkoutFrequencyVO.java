package com.workstation.modules.workout.dto;

import java.util.List;

public record WorkoutFrequencyVO(
        List<CountPointVO> sessions,
        List<PartCountVO> byPart,
        long totalSessions) {
}
