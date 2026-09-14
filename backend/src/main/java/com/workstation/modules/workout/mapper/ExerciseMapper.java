package com.workstation.modules.workout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.workout.entity.Exercise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExerciseMapper extends BaseMapper<Exercise> {

    /** 含已逻辑删除的动作，用于判断同名动作是「没建过」还是「删过」 */
    @Select("SELECT * FROM exercise WHERE part_id = #{partId} AND name = #{name} LIMIT 1")
    Exercise selectIncludingDeleted(@Param("partId") Long partId, @Param("name") String name);

    /**
     * 历史的训练记录可能引用已被删除的动作，展示时仍要能拿到名字，
     * 所以这里绕过逻辑删除把动作字典整个取出来。
     */
    @Select("SELECT * FROM exercise")
    List<Exercise> selectAllIncludingDeleted();

    /** 恢复一个被逻辑删除的动作。updateById 会自动带上 deleted = 0 条件，改不动已删除的行 */
    @Update("UPDATE exercise SET deleted = 0, updated_at = NOW() WHERE id = #{id}")
    int restore(@Param("id") Long id);
}
