package com.workstation.modules.workout;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.workout.dto.WorkoutComparisonVO;
import com.workstation.modules.workout.dto.WorkoutRequest;
import com.workstation.modules.workout.dto.WorkoutVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/workout/records")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping
    public ApiResponse<List<WorkoutVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long partId,
            @RequestParam(required = false) Long exerciseId) {
        return ApiResponse.ok(workoutService.listRecords(start, end, partId, exerciseId));
    }

    /** 放在 /{id} 之前：字面量路径优先于路径变量，避免 "last" 被当成 id 解析 */
    @GetMapping("/last")
    public ApiResponse<WorkoutVO> last(@RequestParam Long exerciseId,
                                       @RequestParam(required = false) Long excludeRecordId) {
        return ApiResponse.ok(workoutService.lastRecord(exerciseId, excludeRecordId));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkoutVO> get(@PathVariable Long id) {
        return ApiResponse.ok(workoutService.getRecord(id));
    }

    @GetMapping("/{id}/comparison")
    public ApiResponse<WorkoutComparisonVO> comparison(@PathVariable Long id) {
        return ApiResponse.ok(workoutService.compareWithPrevious(id));
    }

    @PostMapping
    public ApiResponse<WorkoutVO> create(@Valid @RequestBody WorkoutRequest request) {
        return ApiResponse.ok(workoutService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkoutVO> update(@PathVariable Long id, @Valid @RequestBody WorkoutRequest request) {
        return ApiResponse.ok(workoutService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workoutService.delete(id);
        return ApiResponse.ok();
    }
}
