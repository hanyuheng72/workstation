package com.workstation.modules.task;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.task.dto.CalendarDayVO;
import com.workstation.modules.task.dto.CompletionStatsVO;
import com.workstation.modules.task.dto.TaskOccurrenceVO;
import com.workstation.modules.task.dto.TaskRequest;
import com.workstation.modules.task.dto.TaskVO;
import com.workstation.modules.task.dto.TodayTasksVO;
import com.workstation.modules.task.entity.TaskStatus;
import com.workstation.modules.task.entity.TaskType;
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
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 任务定义列表，用于长期任务、周任务页与编辑管理 */
    @GetMapping
    public ApiResponse<List<TaskVO>> list(@RequestParam(required = false) TaskType type,
                                          @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskService.listTasks(type, status));
    }

    @GetMapping("/today")
    public ApiResponse<TodayTasksVO> today() {
        return ApiResponse.ok(taskService.today());
    }

    /** 某段日期上实际要做的任务（重复任务已展开） */
    @GetMapping("/range")
    public ApiResponse<List<TaskOccurrenceVO>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(taskService.rangeItems(start, end));
    }

    @GetMapping("/calendar")
    public ApiResponse<List<CalendarDayVO>> calendar(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(taskService.calendar(year, month));
    }

    @GetMapping("/stats/completion")
    public ApiResponse<CompletionStatsVO> completion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(taskService.completionStats(start, end));
    }

    @PostMapping
    public ApiResponse<TaskVO> create(@Valid @RequestBody TaskRequest request) {
        return ApiResponse.ok(taskService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskVO> update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ApiResponse.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/occurrences/{date}/complete")
    public ApiResponse<TaskOccurrenceVO> complete(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(taskService.complete(id, date));
    }

    @DeleteMapping("/{id}/occurrences/{date}/complete")
    public ApiResponse<TaskOccurrenceVO> uncomplete(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(taskService.uncomplete(id, date));
    }

    /**
     * 长期任务没有「哪一天」的概念，用这两个接口直接改自身状态。
     * 今日任务与重复任务请用上面的按日期勾选。
     */
    @PostMapping("/{id}/done")
    public ApiResponse<TaskVO> markDone(@PathVariable Long id) {
        return ApiResponse.ok(taskService.markDone(id));
    }

    @DeleteMapping("/{id}/done")
    public ApiResponse<TaskVO> markPending(@PathVariable Long id) {
        return ApiResponse.ok(taskService.markPending(id));
    }
}
