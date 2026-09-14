package com.workstation.modules.dashboard;

import com.workstation.modules.dashboard.dto.DashboardOverviewVO;
import com.workstation.modules.finance.FinanceService;
import com.workstation.modules.finance.dto.FinanceOverviewVO;
import com.workstation.modules.task.TaskService;
import com.workstation.modules.task.dto.TodayTasksVO;
import com.workstation.modules.task.entity.TaskStatus;
import com.workstation.modules.weight.WeightService;
import com.workstation.modules.weight.dto.BmiVO;
import com.workstation.modules.weight.dto.WeightStatsVO;
import com.workstation.modules.workout.WorkoutService;
import com.workstation.modules.workout.WorkoutStatsService;
import com.workstation.modules.workout.dto.WorkoutVO;
import com.workstation.modules.workout.dto.WorkoutVolumeVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 首页聚合。只做编排——每个数字都由对应模块的 Service 算出来，
 * 这里不重复实现任何业务规则，避免仪表盘和后端别处算出两个不同的答案。
 */
@Service
public class DashboardService {

    /** 首页卡片最多列出几条待办，多了放不下也没意义 */
    private static final int MAX_PENDING_PREVIEW = 3;

    private final TaskService taskService;
    private final WeightService weightService;
    private final WorkoutService workoutService;
    private final WorkoutStatsService workoutStatsService;
    private final FinanceService financeService;

    public DashboardService(TaskService taskService, WeightService weightService,
                            WorkoutService workoutService, WorkoutStatsService workoutStatsService,
                            FinanceService financeService) {
        this.taskService = taskService;
        this.weightService = weightService;
        this.workoutService = workoutService;
        this.workoutStatsService = workoutStatsService;
        this.financeService = financeService;
    }

    public DashboardOverviewVO overview() {
        LocalDate today = LocalDate.now();
        return new DashboardOverviewVO(
                today,
                buildTasks(),
                buildWeight(),
                buildWorkout(today),
                buildFinance());
    }

    private DashboardOverviewVO.TaskCardVO buildTasks() {
        TodayTasksVO today = taskService.today();
        List<DashboardOverviewVO.PendingTaskVO> pending = today.items().stream()
                .filter(item -> item.status() == TaskStatus.PENDING)
                .limit(MAX_PENDING_PREVIEW)
                .map(item -> new DashboardOverviewVO.PendingTaskVO(
                        item.taskId(), item.title(), item.occurDate()))
                .toList();
        return new DashboardOverviewVO.TaskCardVO(
                today.total(), today.done(), today.completionRate(), pending);
    }

    private DashboardOverviewVO.WeightCardVO buildWeight() {
        WeightStatsVO stats = weightService.stats();
        if (stats.current() == null) {
            return new DashboardOverviewVO.WeightCardVO(null, null, null, null, null);
        }
        // BMI 复用体重模块的算法与分级，保证与体重页显示的是同一个值
        BmiVO bmi = weightService.bmi();
        return new DashboardOverviewVO.WeightCardVO(
                stats.currentDate(), stats.current(), stats.latestDelta(), bmi.bmi(), bmi.category());
    }

    private DashboardOverviewVO.WorkoutCardVO buildWorkout(LocalDate today) {
        List<WorkoutVO> records = workoutService.listRecords(today, today, null, null);
        if (records.isEmpty()) {
            return new DashboardOverviewVO.WorkoutCardVO(0, List.of(), BigDecimal.ZERO);
        }
        // 按「动作」计数而不是按记录条数：同一个动作分两次练仍然只是一个动作。
        // 容量仍放进返回体，首页不显示，但 AI 总结会用它来判断今天的训练量。
        List<String> names = records.stream().map(WorkoutVO::exerciseName).distinct().toList();
        WorkoutVolumeVO volume = workoutStatsService.volume(today, today);
        return new DashboardOverviewVO.WorkoutCardVO(names.size(), names, volume.totalVolume());
    }

    private DashboardOverviewVO.FinanceCardVO buildFinance() {
        FinanceOverviewVO overview = financeService.overview();
        return new DashboardOverviewVO.FinanceCardVO(
                overview.todayExpense(), overview.weekExpense(),
                overview.monthIncome(), overview.monthExpense(), overview.monthBalance());
    }
}
