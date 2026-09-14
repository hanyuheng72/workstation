package com.workstation.modules.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.task.dto.CalendarDayVO;
import com.workstation.modules.task.dto.CompletionStatsVO;
import com.workstation.modules.task.dto.TaskOccurrenceVO;
import com.workstation.modules.task.dto.TaskRequest;
import com.workstation.modules.task.dto.TaskVO;
import com.workstation.modules.task.dto.TodayTasksVO;
import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.Task;
import com.workstation.modules.task.entity.TaskOccurrence;
import com.workstation.modules.task.entity.TaskStatus;
import com.workstation.modules.task.entity.TaskType;
import com.workstation.modules.task.mapper.TaskMapper;
import com.workstation.modules.task.mapper.TaskOccurrenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Comparator<TaskOccurrenceVO> DISPLAY_ORDER =
            Comparator.comparing((TaskOccurrenceVO vo) -> vo.status() == TaskStatus.PENDING ? 0 : 1)
                    .thenComparing(vo -> vo.priority() == null ? 0 : -vo.priority())
                    .thenComparing(TaskOccurrenceVO::taskId);

    private final TaskMapper taskMapper;
    private final TaskOccurrenceMapper occurrenceMapper;
    private final TaskRecurrenceService recurrenceService;

    public TaskService(TaskMapper taskMapper, TaskOccurrenceMapper occurrenceMapper,
                       TaskRecurrenceService recurrenceService) {
        this.taskMapper = taskMapper;
        this.occurrenceMapper = occurrenceMapper;
        this.recurrenceService = recurrenceService;
    }

    // ---------------- 任务定义 ----------------

    public List<TaskVO> listTasks(TaskType type, TaskStatus status) {
        return taskMapper.selectList(
                        Wrappers.lambdaQuery(Task.class)
                                .eq(type != null, Task::getTaskType, type)
                                .eq(status != null, Task::getStatus, status)
                                .orderByAsc(Task::getStatus)
                                .orderByDesc(Task::getPriority)
                                .orderByDesc(Task::getId))
                .stream().map(TaskService::toTaskVO).toList();
    }

    @Transactional
    public TaskVO create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        task.setStatus(TaskStatus.PENDING);
        taskMapper.insert(task);
        // 立刻补上实例，让新任务马上出现在今天或日历上
        LocalDate anchor = task.anchorDate();
        if (anchor != null) {
            recurrenceService.materialize(List.of(task), anchor, anchor);
        }
        return toTaskVO(task);
    }

    @Transactional
    public TaskVO update(Long id, TaskRequest request) {
        Task task = requireTask(id);
        boolean scheduleChanged = scheduleChanged(task, request);

        apply(task, request);
        taskMapper.updateById(task);

        // 重复规则或落位日期变了：清掉未来还没完成的实例，按新规则重新铺。
        // 已经完成的历史实例保留，否则过去几周的完成率会被改写。
        if (scheduleChanged) {
            occurrenceMapper.delete(Wrappers.lambdaQuery(TaskOccurrence.class)
                    .eq(TaskOccurrence::getTaskId, id)
                    .ne(TaskOccurrence::getStatus, TaskStatus.DONE)
                    .ge(TaskOccurrence::getOccurDate, LocalDate.now()));
            LocalDate anchor = task.anchorDate();
            if (anchor != null) {
                recurrenceService.materialize(List.of(task), anchor, anchor);
            }
        }
        return toTaskVO(task);
    }

    @Transactional
    public void delete(Long id) {
        requireTask(id);
        // task_occurrence 的外键是 ON DELETE CASCADE，这里显式删一次让语义更清楚
        occurrenceMapper.delete(Wrappers.lambdaQuery(TaskOccurrence.class).eq(TaskOccurrence::getTaskId, id));
        taskMapper.deleteById(id);
    }

    // ---------------- 完成状态 ----------------

    @Transactional
    public TaskOccurrenceVO complete(Long taskId, LocalDate date) {
        TaskOccurrence occurrence = requireOccurrence(taskId, date);
        occurrence.setStatus(TaskStatus.DONE);
        occurrence.setCompletedAt(LocalDateTime.now());
        occurrenceMapper.updateById(occurrence);
        syncTaskStatus(taskId);
        return toOccurrenceVO(occurrence, requireTask(taskId));
    }

    @Transactional
    public TaskOccurrenceVO uncomplete(Long taskId, LocalDate date) {
        TaskOccurrence occurrence = requireOccurrence(taskId, date);
        occurrence.setStatus(TaskStatus.PENDING);
        occurrence.setCompletedAt(null);
        occurrenceMapper.updateById(occurrence);
        syncTaskStatus(taskId);
        return toOccurrenceVO(occurrence, requireTask(taskId));
    }

    // ---------------- 长期任务 ----------------

    /**
     * 长期任务没有「哪一天」的概念，所以不走按日期勾选那套，直接改自身状态。
     * 这两个接口只接受长期任务，别的类型传进来会明确报错，避免出现两套状态互相打架。
     */
    @Transactional
    public TaskVO markDone(Long taskId) {
        return setLongTermStatus(taskId, TaskStatus.DONE);
    }

    @Transactional
    public TaskVO markPending(Long taskId) {
        return setLongTermStatus(taskId, TaskStatus.PENDING);
    }

    private TaskVO setLongTermStatus(Long taskId, TaskStatus status) {
        Task task = requireTask(taskId);
        if (!task.isLongTerm()) {
            throw BusinessException.badRequest("只有长期任务用这个方式改状态，其它任务请按日期勾选");
        }
        task.setStatus(status);
        taskMapper.updateById(task);
        return toTaskVO(task);
    }

    // ---------------- 视图 ----------------

    public List<TaskOccurrenceVO> rangeItems(LocalDate from, LocalDate to) {
        List<Task> tasks = taskMapper.selectList(null);
        recurrenceService.materialize(tasks, from, to);

        Map<Long, Task> byId = tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
        return occurrenceMapper.selectList(
                        Wrappers.lambdaQuery(TaskOccurrence.class)
                                .between(TaskOccurrence::getOccurDate, from, to))
                .stream()
                .filter(occurrence -> byId.containsKey(occurrence.getTaskId()))
                .map(occurrence -> toOccurrenceVO(occurrence, byId.get(occurrence.getTaskId())))
                .sorted(DISPLAY_ORDER.thenComparing(TaskOccurrenceVO::occurDate))
                .toList();
    }

    public TodayTasksVO today() {
        LocalDate date = LocalDate.now();
        List<TaskOccurrenceVO> items = rangeItems(date, date);
        int total = items.size();
        int done = countDone(items);
        return new TodayTasksVO(date, items, total, done, rate(done, total));
    }

    public List<CalendarDayVO> calendar(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        List<TaskOccurrenceVO> items = rangeItems(from, to);
        Map<LocalDate, List<TaskOccurrenceVO>> byDate = items.stream()
                .collect(Collectors.groupingBy(TaskOccurrenceVO::occurDate));

        return byDate.entrySet().stream()
                .map(entry -> new CalendarDayVO(entry.getKey(), entry.getValue().size(),
                        countDone(entry.getValue())))
                .sorted(Comparator.comparing(CalendarDayVO::date))
                .toList();
    }

    public CompletionStatsVO completionStats(LocalDate from, LocalDate to) {
        List<TaskOccurrenceVO> items = rangeItems(from, to);
        int total = items.size();
        int done = countDone(items);
        return new CompletionStatsVO(from, to, total, done, rate(done, total));
    }

    // ---------------- 内部 ----------------

    /** 把请求里的日期落到具体值，并校验重复规则自洽 */
    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setTaskType(request.taskType());
        task.setPriority(request.priority() == null ? 0 : request.priority());

        RecurrenceType recurrence = request.recurrenceType() == null ? RecurrenceType.NONE : request.recurrenceType();
        task.setRecurrenceType(recurrence);
        task.setRecurrenceInterval(request.recurrenceInterval() == null ? 1 : request.recurrenceInterval());
        task.setRecurrenceEndDate(request.recurrenceEndDate());

        // planDate 对今日任务与重复任务是「哪一天做 / 从哪天开始重复」；
        // 长期任务不用它生成实例，只是一个排序用的创建日期。
        task.setPlanDate(request.planDate() == null ? LocalDate.now() : request.planDate());
        task.setDueDate(request.dueDate());

        if (recurrence != RecurrenceType.NONE) {
            LocalDate end = task.getRecurrenceEndDate();
            if (end != null && end.isBefore(task.getPlanDate())) {
                throw BusinessException.badRequest("重复结束日期不能早于开始日期");
            }
        }
    }

    private boolean scheduleChanged(Task task, TaskRequest request) {
        RecurrenceType recurrence = request.recurrenceType() == null ? RecurrenceType.NONE : request.recurrenceType();
        Integer interval = request.recurrenceInterval() == null ? 1 : request.recurrenceInterval();
        return task.getRecurrenceType() != recurrence
                || !java.util.Objects.equals(task.getRecurrenceInterval(), interval)
                || !java.util.Objects.equals(task.getRecurrenceEndDate(), request.recurrenceEndDate())
                || !java.util.Objects.equals(task.anchorDate(), requestedAnchor(request));
    }

    private LocalDate requestedAnchor(TaskRequest request) {
        if (request.taskType() == TaskType.LONG_TERM) {
            return null;
        }
        return request.planDate() == null ? LocalDate.now() : request.planDate();
    }

    /**
     * 非重复任务的 task.status 镜像它那唯一一条实例的状态。
     * 长期任务没有实例，它的 status 就是权威值，不参与镜像。
     */
    private void syncTaskStatus(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.isRecurring() || task.isLongTerm()) {
            return;
        }
        TaskOccurrence occurrence = occurrenceMapper.selectOne(
                Wrappers.lambdaQuery(TaskOccurrence.class)
                        .eq(TaskOccurrence::getTaskId, taskId)
                        .eq(TaskOccurrence::getStatus, TaskStatus.DONE)
                        .last("LIMIT 1"));
        TaskStatus next = occurrence == null ? TaskStatus.PENDING : TaskStatus.DONE;
        if (task.getStatus() != next) {
            task.setStatus(next);
            taskMapper.updateById(task);
        }
    }

    private TaskOccurrence requireOccurrence(Long taskId, LocalDate date) {
        Task task = requireTask(taskId);
        LocalDate anchor = task.anchorDate();
        if (!task.isRecurring() && anchor != null && !anchor.equals(date)) {
            throw BusinessException.badRequest("这条任务不在这一天");
        }
        recurrenceService.materialize(List.of(task), date, date);
        TaskOccurrence occurrence = occurrenceMapper.selectOne(
                Wrappers.lambdaQuery(TaskOccurrence.class)
                        .eq(TaskOccurrence::getTaskId, taskId)
                        .eq(TaskOccurrence::getOccurDate, date));
        if (occurrence == null) {
            throw BusinessException.badRequest("这一天没有这条任务");
        }
        return occurrence;
    }

    private Task requireTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("找不到这条任务");
        }
        return task;
    }

    private static int countDone(List<TaskOccurrenceVO> items) {
        return (int) items.stream().filter(vo -> vo.status() == TaskStatus.DONE).count();
    }

    private static int rate(int done, int total) {
        return total == 0 ? 0 : Math.round(done * 100f / total);
    }

    private static TaskVO toTaskVO(Task task) {
        return new TaskVO(task.getId(), task.getTitle(), task.getDescription(), task.getTaskType(),
                task.getPriority(), task.getPlanDate(), task.getDueDate(),
                task.getRecurrenceType(), task.getRecurrenceInterval(), task.getRecurrenceEndDate(),
                task.getStatus(), task.isRecurring(), task.anchorDate());
    }

    private static TaskOccurrenceVO toOccurrenceVO(TaskOccurrence occurrence, Task task) {
        return new TaskOccurrenceVO(
                occurrence.getId(),
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskType(),
                task.getRecurrenceType(),
                task.getPriority(),
                occurrence.getOccurDate(),
                occurrence.getStatus(),
                occurrence.getCompletedAt(),
                task.isRecurring());
    }
}
