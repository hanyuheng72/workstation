package com.workstation.modules.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.finance.dto.CategoryStatRow;
import com.workstation.modules.finance.dto.TypeAmountRow;
import com.workstation.modules.finance.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {

    /** 区间内某个方向的金额合计；没有记录时返回 0 而不是 null */
    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM transaction_record
            WHERE type = #{type} AND occur_date BETWEEN #{from} AND #{to}
            """)
    BigDecimal sumAmount(@Param("type") String type,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    /** 按分类汇总，用于饼图。金额降序，前端不必再排 */
    @Select("""
            SELECT category_id AS categoryId, SUM(amount) AS amount
            FROM transaction_record
            WHERE type = #{type} AND occur_date BETWEEN #{from} AND #{to}
            GROUP BY category_id
            ORDER BY amount DESC
            """)
    List<CategoryStatRow> selectCategoryStats(@Param("type") String type,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /**
     * 按周期（日或月）分方向汇总。
     * GROUP BY 里直接写别名 label，MySQL 允许在 GROUP BY 中引用 SELECT 别名。
     */
    @Select("""
            SELECT DATE_FORMAT(occur_date, #{format}) AS label,
                   type                               AS type,
                   SUM(amount)                        AS amount
            FROM transaction_record
            WHERE occur_date BETWEEN #{from} AND #{to}
            GROUP BY label, type
            ORDER BY label
            """)
    List<TypeAmountRow> selectAmountByPeriod(@Param("format") String format,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
