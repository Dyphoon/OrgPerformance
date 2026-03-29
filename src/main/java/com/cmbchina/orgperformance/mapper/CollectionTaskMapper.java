package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.CollectionTask;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CollectionTaskMapper {

    @Select("SELECT * FROM collection_task WHERE id = #{id}")
    CollectionTask selectById(Long id);

    @Select("SELECT * FROM collection_task WHERE monitoring_id = #{monitoringId}")
    List<CollectionTask> selectByMonitoringId(Long monitoringId);

    @Select("SELECT * FROM collection_task WHERE monitoring_id = #{monitoringId} AND collector_user_id = #{collectorUserId}")
    List<CollectionTask> selectByMonitoringIdAndCollectorUserId(@Param("monitoringId") Long monitoringId,
                                                                @Param("collectorUserId") Long collectorUserId);

    @Select("SELECT * FROM collection_task WHERE monitoring_id = #{monitoringId} AND institution_id = #{institutionId}")
    List<CollectionTask> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                               @Param("institutionId") Long institutionId);

    @Select("SELECT * FROM collection_task WHERE monitoring_id = #{monitoringId} AND status = #{status}")
    List<CollectionTask> selectByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId,
                                                         @Param("status") String status);

    @Select("SELECT COUNT(*) FROM collection_task WHERE monitoring_id = #{monitoringId}")
    int countByMonitoringId(Long monitoringId);

    @Select("SELECT COUNT(*) FROM collection_task WHERE monitoring_id = #{monitoringId} AND status = #{status}")
    int countByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId, @Param("status") String status);

    @Insert("<script>" +
            "INSERT INTO collection_task (monitoring_id, indicator_id, institution_id, collector_name, " +
            "collector_emp_no, collector_user_id, status, file_key, " +
            "collection_indicator_name, collection_unit, collection_dimension, collection_category) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.monitoringId}, #{item.indicatorId}, #{item.institutionId}, #{item.collectorName}, " +
            "#{item.collectorEmpNo}, #{item.collectorUserId}, #{item.status}, #{item.fileKey}, " +
            "#{item.collectionIndicatorName}, #{item.collectionUnit}, #{item.collectionDimension}, #{item.collectionCategory})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<CollectionTask> tasks);

    @Update("UPDATE collection_task SET actual_value=#{actualValue}, status=#{status}, submitted_at=NOW() WHERE id=#{id}")
    int submitTask(@Param("id") Long id, @Param("actualValue") BigDecimal actualValue, @Param("status") String status);

    @Update("UPDATE collection_task SET status=#{status}, approved_by=#{approvedBy}, approved_at=NOW() WHERE id=#{id}")
    int approveTask(@Param("id") Long id, @Param("status") String status, @Param("approvedBy") String approvedBy);

    @Update("UPDATE collection_task SET status=#{status}, remark=#{remark}, approved_by=#{approvedBy}, approved_at=NOW() WHERE id=#{id}")
    int rejectTask(@Param("id") Long id, @Param("status") String status, @Param("remark") String remark,
                    @Param("approvedBy") String approvedBy);

    @Delete("DELETE FROM collection_task WHERE monitoring_id = #{monitoringId}")
    int deleteByMonitoringId(Long monitoringId);

    @Select("SELECT DISTINCT collector_emp_no FROM collection_task WHERE monitoring_id = #{monitoringId}")
    List<String> selectDistinctCollectors(Long monitoringId);

    @Update("UPDATE collection_task SET collector_name=#{collectorName}, collector_emp_no=#{collectorEmpNo}, " +
            "collector_user_id=#{collectorUserId} WHERE monitoring_id=#{monitoringId} AND institution_id=#{institutionId} " +
            "AND (collector_user_id IS NULL OR collector_user_id = 0)")
    int updateCollectorByInstitutionId(@Param("institutionId") Long institutionId,
                                      @Param("monitoringId") Long monitoringId,
                                      @Param("collectorName") String collectorName,
                                      @Param("collectorEmpNo") String collectorEmpNo,
                                      @Param("collectorUserId") Long collectorUserId);

    @Update("UPDATE collection_task SET file_key=#{fileKey} WHERE monitoring_id=#{monitoringId} AND collector_user_id=#{collectorUserId}")
    int updateFileKeyByCollector(@Param("monitoringId") Long monitoringId,
                                @Param("collectorUserId") Long collectorUserId,
                                @Param("fileKey") String fileKey);

    @Select("SELECT * FROM collection_task WHERE monitoring_id = #{monitoringId} AND collector_user_id = #{collectorUserId} AND institution_id = #{institutionId} LIMIT 1")
    CollectionTask selectByMonitoringCollectorAndInstitution(@Param("monitoringId") Long monitoringId,
                                                            @Param("collectorUserId") Long collectorUserId,
                                                            @Param("institutionId") Long institutionId);
}
