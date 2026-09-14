package com.workstation.modules.workout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.workout.dto.PartSessionCount;
import com.workstation.modules.workout.dto.SessionTrendRow;
import com.workstation.modules.workout.entity.WorkoutRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkoutRecordMapper extends BaseMapper<WorkoutRecord> {

    /** 各部位累计训练次数（同一天同一部位算一次） */
    @Select("""
            SELECT part_id AS partId, COUNT(DISTINCT record_date) AS sessionCount
            FROM workout_record
            GROUP BY part_id
            """)
    List<PartSessionCount> countSessionsByPart();

    /**
     * 训练次数趋势。同一天练多个部位仍然只算一次训练，
     * 所以这里 COUNT 的是 DISTINCT record_date 而不是记录条数。
     */
    @Select("""
            SELECT DATE_FORMAT(record_date, #{format}) AS label,
                   COUNT(DISTINCT record_date)         AS value,
                   MIN(record_date)                    AS sampleDate
            FROM workout_record
            WHERE record_date >= #{from}
            GROUP BY label
            ORDER BY label
            """)
    List<SessionTrendRow> selectSessionTrend(@Param("format") String format, @Param("from") LocalDate from);
}
