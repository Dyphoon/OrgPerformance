package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.MonthlyIndicatorData;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MonthlyIndicatorDataMapper {

    @Select("SELECT * FROM monthly_indicator_data WHERE id = #{id}")
    MonthlyIndicatorData selectById(Long id);

    @Select("SELECT * FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId}")
    List<MonthlyIndicatorData> selectByMonitoringId(Long monitoringId);

    @Select("SELECT * FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId} AND institution_id = #{institutionId}")
    List<MonthlyIndicatorData> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                                     @Param("institutionId") Long institutionId);

    @Select("SELECT * FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId} AND indicator_id = #{indicatorId}")
    List<MonthlyIndicatorData> selectByMonitoringIdAndIndicatorId(@Param("monitoringId") Long monitoringId,
                                                                   @Param("indicatorId") Long indicatorId);

    @Insert("<script>" +
            "INSERT INTO monthly_indicator_data (monitoring_id, indicator_id, institution_id, actual_value, " +
            "annual_target, progress_target, annual_completion_rate, progress_completion_rate, " +
            "score_100, score_weighted, score_category, score_dimension, total_score, file_key) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.monitoringId}, #{item.indicatorId}, #{item.institutionId}, #{item.actualValue}, " +
            "#{item.annualTarget}, #{item.progressTarget}, " +
            "#{item.annualCompletionRate}, #{item.progressCompletionRate}, " +
            "#{item.score100}, #{item.scoreWeighted}, #{item.scoreCategory}, #{item.scoreDimension}, " +
            "#{item.totalScore}, #{item.fileKey})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<MonthlyIndicatorData> dataList);

    @Delete("DELETE FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId}")
    int deleteByMonitoringId(Long monitoringId);

    @Delete("DELETE FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId} AND institution_id = #{institutionId}")
    int deleteByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId, @Param("institutionId") Long institutionId);

    @Select("SELECT * FROM monthly_indicator_data mid " +
            "JOIN indicator i ON mid.indicator_id = i.id " +
            "WHERE mid.monitoring_id = #{monitoringId} AND mid.institution_id = #{institutionId} " +
            "ORDER BY i.dimension, i.category, i.row_index")
    List<MonthlyIndicatorData> selectDetailByInstitution(@Param("monitoringId") Long monitoringId,
                                                        @Param("institutionId") Long institutionId);

    @Select("SELECT file_key FROM monthly_indicator_data WHERE monitoring_id = #{monitoringId} AND institution_id = #{institutionId} LIMIT 1")
    String selectFileKeyByMonitoringAndInstitution(@Param("monitoringId") Long monitoringId,
                                                    @Param("institutionId") Long institutionId);
}
