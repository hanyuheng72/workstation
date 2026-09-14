package com.workstation.modules.weight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.weight.dto.WeightTrendRow;
import com.workstation.modules.weight.entity.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    /**
     * 按天/月/年聚合的体重趋势。同一天只有一条记录，所以 DAY 粒度下 AVG 等同于原值，
     * 但 MONTH / YEAR 粒度下 AVG 才是正确的「该月/该年的平均体重」。
     */
    @Select("""
            SELECT DATE_FORMAT(record_date, #{format}) AS label,
                   ROUND(AVG(weight_kg), 2)            AS value,
                   MIN(record_date)                    AS sampleDate
            FROM weight_record
            WHERE record_date >= #{from}
            GROUP BY label
            ORDER BY label
            """)
    List<WeightTrendRow> selectTrend(@Param("format") String format, @Param("from") LocalDate from);
}
