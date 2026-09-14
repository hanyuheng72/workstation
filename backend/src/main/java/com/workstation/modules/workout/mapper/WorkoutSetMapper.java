package com.workstation.modules.workout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.workout.dto.PersonalBestRow;
import com.workstation.modules.workout.dto.WorkoutVolumeRow;
import com.workstation.modules.workout.entity.WorkoutSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkoutSetMapper extends BaseMapper<WorkoutSet> {

    /**
     * 区间内的训练量。
     * 有氧组的 weight_kg / reps 是 null，乘出来也是 null，SUM 会自动忽略——
     * 也就是说容量天然只统计力量训练，不需要额外过滤条件。
     */
    @Select("""
            SELECT COALESCE(SUM(s.weight_kg * s.reps), 0) AS totalVolume,
                   COALESCE(SUM(s.reps), 0)               AS totalReps,
                   COUNT(s.id)                            AS totalSets,
                   COUNT(DISTINCT r.id)                   AS totalRecords
            FROM workout_record r
            JOIN workout_set s ON s.record_id = r.id
            WHERE r.record_date BETWEEN #{from} AND #{to}
            """)
    WorkoutVolumeRow selectVolume(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 每个动作的历史最大重量，以及达成它的那一组的次数与日期。
     * 用窗口函数取「按重量降序排第一的那一组」，比先 MAX 再回查少一次往返。
     */
    @Select("""
            SELECT exerciseId, maxWeight, reps, achievedDate FROM (
                SELECT r.exercise_id AS exerciseId,
                       s.weight_kg   AS maxWeight,
                       s.reps        AS reps,
                       r.record_date AS achievedDate,
                       ROW_NUMBER() OVER (
                           PARTITION BY r.exercise_id
                           ORDER BY s.weight_kg DESC, s.reps DESC, r.record_date DESC
                       ) AS rn
                FROM workout_record r
                JOIN workout_set s ON s.record_id = r.id
                WHERE s.weight_kg IS NOT NULL
            ) ranked
            WHERE rn = 1
            ORDER BY maxWeight DESC
            """)
    List<PersonalBestRow> selectPersonalBests();
}
