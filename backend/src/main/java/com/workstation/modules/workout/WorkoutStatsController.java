package com.workstation.modules.workout;

import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ApiResponse;
import com.workstation.modules.workout.dto.PersonalBestVO;
import com.workstation.modules.workout.dto.WorkoutFrequencyVO;
import com.workstation.modules.workout.dto.WorkoutVolumeVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/workout/stats")
public class WorkoutStatsController {

    private static final Set<String> FREQUENCY_GRANULARITIES = Set.of("week", "month");

    private final WorkoutStatsService statsService;

    public WorkoutStatsController(WorkoutStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/frequency")
    public ApiResponse<WorkoutFrequencyVO> frequency(
            @RequestParam(defaultValue = "week") String granularity) {
        String value = granularity.trim().toLowerCase();
        if (!FREQUENCY_GRANULARITIES.contains(value)) {
            throw BusinessException.badRequest("granularity 只支持 week / month");
        }
        return ApiResponse.ok(statsService.frequency(value));
    }

    @GetMapping("/volume")
    public ApiResponse<WorkoutVolumeVO> volume(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(statsService.volume(start, end));
    }

    @GetMapping("/pr")
    public ApiResponse<List<PersonalBestVO>> personalBests() {
        return ApiResponse.ok(statsService.personalBests());
    }
}
