package com.workstation.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.task.entity.TaskOccurrence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskOccurrenceMapper extends BaseMapper<TaskOccurrence> {

    /**
     * 幂等补齐任务实例。唯一键 uk_task_date 让重复插入被静默跳过，
     * 因此这个接口可以放心地重复调用，不需要先查再插。
     * status 走列默认值 PENDING。
     */
    @Insert("""
            <script>
            INSERT IGNORE INTO task_occurrence (task_id, occur_date, created_at, updated_at) VALUES
            <foreach collection="list" item="item" separator=",">
                (#{item.taskId}, #{item.occurDate}, NOW(), NOW())
            </foreach>
            </script>
            """)
    int insertIgnoreBatch(@Param("list") List<TaskOccurrence> occurrences);
}
