package com.workstation.modules.workout;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.workout.dto.ExerciseCreateRequest;
import com.workstation.modules.workout.dto.ExercisePartVO;
import com.workstation.modules.workout.dto.ExerciseVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workout")
public class WorkoutDictionaryController {

    private final WorkoutService workoutService;

    public WorkoutDictionaryController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/parts")
    public ApiResponse<List<ExercisePartVO>> parts() {
        return ApiResponse.ok(workoutService.listParts());
    }

    @GetMapping("/parts/{partId}/exercises")
    public ApiResponse<List<ExerciseVO>> exercises(@PathVariable Long partId) {
        return ApiResponse.ok(workoutService.listExercises(partId));
    }

    @PostMapping("/exercises")
    public ApiResponse<ExerciseVO> createExercise(@Valid @RequestBody ExerciseCreateRequest request) {
        return ApiResponse.ok(workoutService.createExercise(request));
    }

    @DeleteMapping("/exercises/{id}")
    public ApiResponse<Void> deleteExercise(@PathVariable Long id) {
        workoutService.deleteExercise(id);
        return ApiResponse.ok();
    }
}
