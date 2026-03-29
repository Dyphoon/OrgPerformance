package com.cmbchina.termgoal.mapper;

import com.cmbchina.termgoal.entity.Indicator;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface IndicatorMapper {

    @Select("SELECT * FROM indicator WHERE id = #{id}")
    Indicator selectById(Long id);

    @Select("SELECT * FROM indicator WHERE system_id = #{systemId} ORDER BY row_index")
    List<Indicator> selectBySystemId(Long systemId);

    @Select("SELECT * FROM indicator WHERE system_id = #{systemId} AND dimension = #{dimension} ORDER BY row_index")
    List<Indicator> selectBySystemIdAndDimension(@Param("systemId") Long systemId, @Param("dimension") String dimension);

    @Insert("<script>" +
            "INSERT INTO indicator (system_id, dimension, category, level1_name, level2_name, weight, unit, " +
            "annual_target, progress_target, row_index) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.systemId}, #{item.dimension}, #{item.category}, #{item.level1Name}, #{item.level2Name}, " +
            "#{item.weight}, #{item.unit}, #{item.annualTarget}, #{item.progressTarget}, #{item.rowIndex})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<Indicator> indicators);

    @Delete("DELETE FROM indicator WHERE system_id = #{systemId}")
    int deleteBySystemId(Long systemId);

    @Select("SELECT DISTINCT dimension FROM indicator WHERE system_id = #{systemId}")
    List<String> selectDimensionsBySystemId(Long systemId);

    @Select("SELECT DISTINCT category FROM indicator WHERE system_id = #{systemId} AND dimension = #{dimension}")
    List<String> selectCategoriesByDimension(@Param("systemId") Long systemId, @Param("dimension") String dimension);
}
