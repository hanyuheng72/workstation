package com.workstation.modules.workout;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.workout.dto.ExerciseCreateRequest;
import com.workstation.modules.workout.dto.ExercisePartVO;
import com.workstation.modules.workout.dto.ExerciseVO;
import com.workstation.modules.workout.dto.PartSessionCount;
import com.workstation.modules.workout.dto.WorkoutComparisonVO;
import com.workstation.modules.workout.dto.WorkoutRequest;
import com.workstation.modules.workout.dto.WorkoutSetRequest;
import com.workstation.modules.workout.dto.WorkoutSetVO;
import com.workstation.modules.workout.dto.WorkoutVO;
import com.workstation.modules.workout.entity.Exercise;
import com.workstation.modules.workout.entity.ExercisePart;
import com.workstation.modules.workout.entity.WorkoutRecord;
import com.workstation.modules.workout.entity.WorkoutSet;
import com.workstation.modules.workout.mapper.ExerciseMapper;
import com.workstation.modules.workout.mapper.ExercisePartMapper;
import com.workstation.modules.workout.mapper.WorkoutRecordMapper;
import com.workstation.modules.workout.mapper.WorkoutSetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkoutService {

    private final ExercisePartMapper partMapper;
    private final ExerciseMapper exerciseMapper;
    private final WorkoutRecordMapper recordMapper;
    private final WorkoutSetMapper setMapper;

    public WorkoutService(ExercisePartMapper partMapper, ExerciseMapper exerciseMapper,
                          WorkoutRecordMapper recordMapper, WorkoutSetMapper setMapper) {
        this.partMapper = partMapper;
        this.exerciseMapper = exerciseMapper;
        this.recordMapper = recordMapper;
        this.setMapper = setMapper;
    }

    // ---------------- 字典 ----------------

    public List<ExercisePartVO> listParts() {
        Map<Long, Long> sessions = recordMapper.countSessionsByPart().stream()
                .collect(Collectors.toMap(PartSessionCount::partId, PartSessionCount::sessionCount));

        List<ExercisePart> parts = partMapper.selectList(
                Wrappers.lambdaQuery(ExercisePart.class).orderByAsc(ExercisePart::getSortOrder));

        Map<Long, Long> exerciseCounts = exerciseMapper
                .selectList(Wrappers.lambdaQuery(Exercise.class))
                .stream()
                .collect(Collectors.groupingBy(Exercise::getPartId, Collectors.counting()));

        return parts.stream()
                .map(part -> new ExercisePartVO(
                        part.getId(), part.getCode(), part.getName(), part.getSortOrder(), part.isCardio(),
                        sessions.getOrDefault(part.getId(), 0L),
                        exerciseCounts.getOrDefault(part.getId(), 0L)))
                .toList();
    }

    public List<ExerciseVO> listExercises(Long partId) {
        requirePart(partId);
        return exerciseMapper.selectList(
                        Wrappers.lambdaQuery(Exercise.class)
                                .eq(Exercise::getPartId, partId)
                                .orderByAsc(Exercise::getIsDefault)
                                .orderByAsc(Exercise::getId))
                .stream().map(WorkoutService::toExerciseVO).toList();
    }

    /**
     * 按名字找动作，供 AI 解析出的自然语言使用。
     * 先精确匹配，再退化到包含匹配（用户可能说「卧推」而字典里是「上斜卧推」）。
     * 找不到返回 null，由调用方决定怎么提示。
     */
    public ExerciseVO findExerciseByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String keyword = name.trim();

        Exercise exact = exerciseMapper.selectOne(
                Wrappers.lambdaQuery(Exercise.class)
                        .eq(Exercise::getName, keyword)
                        .last("LIMIT 1"));
        if (exact != null) {
            return toExerciseVO(exact);
        }

        return exerciseMapper.selectList(
                        Wrappers.lambdaQuery(Exercise.class)
                                .like(Exercise::getName, keyword)
                                .orderByAsc(Exercise::getId)
                                .last("LIMIT 1"))
                .stream().findFirst().map(WorkoutService::toExerciseVO).orElse(null);
    }

    /**
     * 同名动作若之前被删过，走「恢复」而不是新建，
     * 这样历史训练记录里的动作关联不会断。
     */
    @Transactional
    public ExerciseVO createExercise(ExerciseCreateRequest request) {
        requirePart(request.partId());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw BusinessException.badRequest("请填写动作名称");
        }

        Exercise existing = exerciseMapper.selectIncludingDeleted(request.partId(), name);
        if (existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
            throw BusinessException.conflict("这个部位下已经有「" + name + "」了");
        }
        if (existing != null) {
            exerciseMapper.restore(existing.getId());
            existing.setDeleted(false);
            return toExerciseVO(existing);
        }

        Exercise exercise = new Exercise();
        exercise.setPartId(request.partId());
        exercise.setName(name);
        exercise.setIsDefault(false);
        exercise.setDeleted(false);
        exerciseMapper.insert(exercise);
        return toExerciseVO(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw BusinessException.notFound("找不到这个动作");
        }
        exerciseMapper.deleteById(id);
    }

    // ---------------- 训练记录 ----------------

    public List<WorkoutVO> listRecords(LocalDate start, LocalDate end, Long partId, Long exerciseId) {
        List<WorkoutRecord> records = recordMapper.selectList(
                Wrappers.lambdaQuery(WorkoutRecord.class)
                        .ge(start != null, WorkoutRecord::getRecordDate, start)
                        .le(end != null, WorkoutRecord::getRecordDate, end)
                        .eq(partId != null, WorkoutRecord::getPartId, partId)
                        .eq(exerciseId != null, WorkoutRecord::getExerciseId, exerciseId)
                        .orderByDesc(WorkoutRecord::getRecordDate)
                        .orderByDesc(WorkoutRecord::getId));
        return assemble(records);
    }

    public WorkoutVO getRecord(Long id) {
        WorkoutRecord record = requireRecord(id);
        return assemble(List.of(record)).get(0);
    }

    @Transactional
    public WorkoutVO create(WorkoutRequest request) {
        Exercise exercise = requireExercise(request.exerciseId());
        ExercisePart part = requirePart(exercise.getPartId());
        validateSets(part, request.sets());

        WorkoutRecord record = new WorkoutRecord();
        record.setRecordDate(request.recordDate() == null ? LocalDate.now() : request.recordDate());
        record.setPartId(part.getId());
        record.setExerciseId(exercise.getId());
        record.setNote(request.note());
        recordMapper.insert(record);

        insertSets(record.getId(), request.sets());
        return assemble(List.of(record)).get(0);
    }

    @Transactional
    public WorkoutVO update(Long id, WorkoutRequest request) {
        WorkoutRecord record = requireRecord(id);
        Exercise exercise = requireExercise(request.exerciseId());
        ExercisePart part = requirePart(exercise.getPartId());
        validateSets(part, request.sets());

        record.setRecordDate(request.recordDate() == null ? record.getRecordDate() : request.recordDate());
        record.setPartId(part.getId());
        record.setExerciseId(exercise.getId());
        record.setNote(request.note());
        recordMapper.updateById(record);

        // 整组替换：组数、次数、重量的改动都可能涉及增删
        setMapper.delete(Wrappers.lambdaQuery(WorkoutSet.class).eq(WorkoutSet::getRecordId, id));
        insertSets(id, request.sets());
        return assemble(List.of(record)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        requireRecord(id);
        // workout_set 的外键是 ON DELETE CASCADE，这里显式删一次让语义更清楚
        setMapper.delete(Wrappers.lambdaQuery(WorkoutSet.class).eq(WorkoutSet::getRecordId, id));
        recordMapper.deleteById(id);
    }

    /** 上一次同一动作的记录（排除指定记录本身） */
    public WorkoutVO lastRecord(Long exerciseId, Long excludeRecordId) {
        WorkoutRecord record = recordMapper.selectOne(
                Wrappers.lambdaQuery(WorkoutRecord.class)
                        .eq(WorkoutRecord::getExerciseId, exerciseId)
                        .ne(excludeRecordId != null, WorkoutRecord::getId, excludeRecordId)
                        .orderByDesc(WorkoutRecord::getRecordDate)
                        .orderByDesc(WorkoutRecord::getId)
                        .last("LIMIT 1"));
        return record == null ? null : assemble(List.of(record)).get(0);
    }

    /**
     * 与上一次同动作对比。有氧不比重量的数值，只给出总时长的变化。
     */
    public WorkoutComparisonVO compareWithPrevious(Long recordId) {
        WorkoutRecord record = requireRecord(recordId);
        ExercisePart part = requirePart(record.getPartId());
        WorkoutVO current = assemble(List.of(record)).get(0);
        WorkoutVO previous = lastRecord(record.getExerciseId(), recordId);

        if (previous == null) {
            return WorkoutComparisonVO.none();
        }

        if (part.isCardio()) {
            BigDecimal currentMinutes = sumDuration(current.sets());
            BigDecimal previousMinutes = sumDuration(previous.sets());
            BigDecimal delta = currentMinutes.subtract(previousMinutes);
            return new WorkoutComparisonVO(true, previous.recordDate(), null, null, null,
                    previousMinutes, currentMinutes, delta, describeDuration(delta));
        }

        BigDecimal currentMax = current.maxWeight();
        BigDecimal previousMax = previous.maxWeight();
        BigDecimal weightDelta = currentMax.subtract(previousMax);
        BigDecimal volumeDelta = current.totalVolume().subtract(previous.totalVolume());

        return new WorkoutComparisonVO(true, previous.recordDate(),
                previousMax, currentMax, weightDelta,
                previous.totalVolume(), current.totalVolume(), volumeDelta,
                describeWeight(weightDelta, currentMax, previousMax));
    }

    // ---------------- 内部 ----------------

    /**
     * 力量动作必须有重量和次数；有氧必须有时长或距离。
     * 这条规则依赖部位，所以只能在这里校验，注解做不到。
     */
    private void validateSets(ExercisePart part, List<WorkoutSetRequest> sets) {
        for (int index = 0; index < sets.size(); index++) {
            WorkoutSetRequest set = sets.get(index);
            String label = "第 " + (index + 1) + " 组";

            if (part.isCardio()) {
                if (set.durationMin() == null && set.distanceKm() == null) {
                    throw BusinessException.badRequest(label + "请填写时长或距离");
                }
            } else {
                if (set.weightKg() == null) {
                    throw BusinessException.badRequest(label + "请填写重量");
                }
                if (set.reps() == null || set.reps() <= 0) {
                    throw BusinessException.badRequest(label + "请填写次数");
                }
                if (set.weightKg().signum() < 0) {
                    throw BusinessException.badRequest(label + "重量不能为负数");
                }
            }
        }
    }

    private void insertSets(Long recordId, List<WorkoutSetRequest> requests) {
        for (int index = 0; index < requests.size(); index++) {
            WorkoutSetRequest request = requests.get(index);
            WorkoutSet set = new WorkoutSet();
            set.setRecordId(recordId);
            set.setSetIndex(index + 1);
            set.setWeightKg(request.weightKg());
            set.setReps(request.reps());
            set.setDurationMin(request.durationMin());
            set.setDistanceKm(request.distanceKm());
            setMapper.insert(set);
        }
    }

    /** 批量装配，避免逐条记录查动作/部位/组造成 N+1 */
    private List<WorkoutVO> assemble(List<WorkoutRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        Map<Long, Exercise> exercises = exerciseIndex();
        Map<Long, ExercisePart> parts = partMapper.selectList(null).stream()
                .collect(Collectors.toMap(ExercisePart::getId, Function.identity()));

        List<Long> recordIds = records.stream().map(WorkoutRecord::getId).toList();
        Map<Long, List<WorkoutSet>> setsByRecord = setMapper.selectList(
                        Wrappers.lambdaQuery(WorkoutSet.class)
                                .in(WorkoutSet::getRecordId, recordIds)
                                .orderByAsc(WorkoutSet::getSetIndex))
                .stream().collect(Collectors.groupingBy(WorkoutSet::getRecordId));

        return records.stream().map(record -> {
            Exercise exercise = exercises.get(record.getExerciseId());
            ExercisePart part = parts.get(record.getPartId());
            List<WorkoutSet> sets = setsByRecord.getOrDefault(record.getId(), List.of());
            return toWorkoutVO(record, part, exercise, sets);
        }).toList();
    }

    private Map<Long, Exercise> exerciseIndex() {
        return exerciseMapper.selectAllIncludingDeleted().stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }

    private static WorkoutVO toWorkoutVO(WorkoutRecord record, ExercisePart part, Exercise exercise,
                                         List<WorkoutSet> sets) {
        boolean cardio = part != null && part.isCardio();

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal maxWeight = null;
        int totalReps = 0;

        for (WorkoutSet set : sets) {
            if (set.getReps() != null) {
                totalReps += set.getReps();
            }
            if (set.getWeightKg() != null && set.getReps() != null) {
                totalVolume = totalVolume.add(set.getWeightKg().multiply(BigDecimal.valueOf(set.getReps())));
            }
            if (set.getWeightKg() != null && (maxWeight == null || set.getWeightKg().compareTo(maxWeight) > 0)) {
                maxWeight = set.getWeightKg();
            }
        }

        return new WorkoutVO(
                record.getId(),
                record.getRecordDate(),
                record.getPartId(),
                part == null ? null : part.getName(),
                part == null ? null : part.getCode(),
                cardio,
                record.getExerciseId(),
                exercise == null ? null : exercise.getName(),
                record.getNote(),
                sets.stream().map(WorkoutService::toSetVO).toList(),
                totalVolume,
                maxWeight,
                totalReps);
    }

    private static WorkoutSetVO toSetVO(WorkoutSet set) {
        return new WorkoutSetVO(set.getId(), set.getSetIndex(), set.getWeightKg(), set.getReps(),
                set.getDurationMin(), set.getDistanceKm());
    }

    private static ExerciseVO toExerciseVO(Exercise exercise) {
        return new ExerciseVO(exercise.getId(), exercise.getPartId(), exercise.getName(), exercise.getIsDefault());
    }

    private BigDecimal sumDuration(List<WorkoutSetVO> sets) {
        return sets.stream()
                .map(WorkoutSetVO::durationMin)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String describeWeight(BigDecimal delta, BigDecimal currentMax, BigDecimal previousMax) {
        BigDecimal abs = delta.abs().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        int cmp = delta.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            return "重量较上次提升 " + abs.toPlainString() + "kg";
        }
        if (cmp < 0) {
            return "重量较上次下降 " + abs.toPlainString() + "kg";
        }
        return "重量与上次持平（" + currentMax.stripTrailingZeros().toPlainString() + "kg）";
    }

    private static String describeDuration(BigDecimal delta) {
        BigDecimal abs = delta.abs().setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        int cmp = delta.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            return "时长较上次增加 " + abs.toPlainString() + " 分钟";
        }
        if (cmp < 0) {
            return "时长较上次减少 " + abs.toPlainString() + " 分钟";
        }
        return "时长与上次持平";
    }

    private ExercisePart requirePart(Long partId) {
        ExercisePart part = partMapper.selectById(partId);
        if (part == null) {
            throw BusinessException.notFound("找不到这个部位");
        }
        return part;
    }

    private Exercise requireExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw BusinessException.notFound("找不到这个动作");
        }
        return exercise;
    }

    private WorkoutRecord requireRecord(Long id) {
        WorkoutRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("找不到这条训练记录");
        }
        return record;
    }
}
