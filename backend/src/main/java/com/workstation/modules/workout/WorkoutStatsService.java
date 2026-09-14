package com.workstation.modules.workout;

import com.workstation.modules.workout.dto.CountPointVO;
import com.workstation.modules.workout.dto.PartCountVO;
import com.workstation.modules.workout.dto.PersonalBestRow;
import com.workstation.modules.workout.dto.PersonalBestVO;
import com.workstation.modules.workout.dto.SessionTrendRow;
import com.workstation.modules.workout.dto.WorkoutFrequencyVO;
import com.workstation.modules.workout.dto.WorkoutVolumeRow;
import com.workstation.modules.workout.dto.WorkoutVolumeVO;
import com.workstation.modules.workout.entity.Exercise;
import com.workstation.modules.workout.entity.ExercisePart;
import com.workstation.modules.workout.mapper.ExerciseMapper;
import com.workstation.modules.workout.mapper.ExercisePartMapper;
import com.workstation.modules.workout.mapper.WorkoutRecordMapper;
import com.workstation.modules.workout.mapper.WorkoutSetMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkoutStatsService {

    private final WorkoutRecordMapper recordMapper;
    private final WorkoutSetMapper setMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExercisePartMapper partMapper;

    public WorkoutStatsService(WorkoutRecordMapper recordMapper, WorkoutSetMapper setMapper,
                               ExerciseMapper exerciseMapper, ExercisePartMapper partMapper) {
        this.recordMapper = recordMapper;
        this.setMapper = setMapper;
        this.exerciseMapper = exerciseMapper;
        this.partMapper = partMapper;
    }

    /**
     * 训练频次。week = 最近 12 周，month = 最近 12 个月。
     * 同一周/月内练多个部位只算一次训练，所以统计的是「有训练的天数」。
     */
    public WorkoutFrequencyVO frequency(String granularity) {
        boolean weekly = !"month".equalsIgnoreCase(granularity);
        LocalDate today = LocalDate.now();

        String format = weekly ? "%x-W%v" : "%Y-%m";
        LocalDate from = weekly ? today.minusWeeks(11) : today.withDayOfMonth(1).minusMonths(11);

        List<CountPointVO> sessions = recordMapper.selectSessionTrend(format, from).stream()
                .map(row -> new CountPointVO(row.label(), labelOf(row, weekly), row.value()))
                .toList();

        Map<Long, Long> counts = recordMapper.countSessionsByPart().stream()
                .collect(Collectors.toMap(c -> c.partId(), c -> c.sessionCount()));

        List<PartCountVO> byPart = partMapper.selectList(null).stream()
                .sorted((a, b) -> Long.compare(
                        counts.getOrDefault(b.getId(), 0L), counts.getOrDefault(a.getId(), 0L)))
                .map(part -> new PartCountVO(part.getId(), part.getName(), part.getCode(),
                        counts.getOrDefault(part.getId(), 0L)))
                .toList();

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new WorkoutFrequencyVO(sessions, byPart, total);
    }

    public WorkoutVolumeVO volume(LocalDate start, LocalDate end) {
        LocalDate to = end == null ? LocalDate.now() : end;
        LocalDate from = start == null ? to.withDayOfMonth(1) : start;

        WorkoutVolumeRow row = setMapper.selectVolume(from, to);
        return new WorkoutVolumeVO(
                from, to,
                row == null || row.totalVolume() == null ? BigDecimal.ZERO : row.totalVolume(),
                row == null || row.totalReps() == null ? 0 : row.totalReps(),
                row == null || row.totalSets() == null ? 0 : row.totalSets(),
                row == null || row.totalRecords() == null ? 0 : row.totalRecords());
    }

    /** 每个动作的历史最大重量。动作可能已被逻辑删除，所以用「含已删除」的字典来取名字 */
    public List<PersonalBestVO> personalBests() {
        List<PersonalBestRow> rows = setMapper.selectPersonalBests();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Exercise> exercises = exerciseMapper.selectAllIncludingDeleted().stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        Map<Long, String> partNames = partMapper.selectList(null).stream()
                .collect(Collectors.toMap(ExercisePart::getId, ExercisePart::getName));

        return rows.stream().map(row -> {
            Exercise exercise = exercises.get(row.exerciseId());
            return new PersonalBestVO(
                    row.exerciseId(),
                    exercise == null ? "已删除的动作" : exercise.getName(),
                    exercise == null ? null : partNames.get(exercise.getPartId()),
                    row.maxWeight(),
                    row.reps(),
                    row.achievedDate());
        }).toList();
    }

    private static String labelOf(SessionTrendRow row, boolean weekly) {
        LocalDate sample = row.sampleDate();
        if (sample == null) {
            return row.label();
        }
        // 周：标这一周的第一天；月：标月份
        return weekly
                ? sample.getMonthValue() + "/" + sample.getDayOfMonth()
                : sample.getMonthValue() + "月";
    }
}
