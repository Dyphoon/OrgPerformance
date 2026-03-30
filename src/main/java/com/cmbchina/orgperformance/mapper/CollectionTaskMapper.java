package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.CollectionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CollectionTaskMapper {

    CollectionTask selectById(Long id);

    List<CollectionTask> selectByMonitoringId(Long monitoringId);

    List<CollectionTask> selectByMonitoringIdAndCollectorUserId(@Param("monitoringId") Long monitoringId,
                                                                @Param("collectorUserId") Long collectorUserId);

    List<CollectionTask> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                               @Param("institutionId") Long institutionId);

    List<CollectionTask> selectByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId,
                                                         @Param("status") String status);

    int countByMonitoringId(Long monitoringId);

    int countByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId, @Param("status") String status);

    List<String> selectDistinctCollectors(Long monitoringId);

    CollectionTask selectByMonitoringCollectorAndInstitution(@Param("monitoringId") Long monitoringId,
                                                            @Param("collectorUserId") Long collectorUserId,
                                                            @Param("institutionId") Long institutionId);

    int batchInsert(@Param("list") List<CollectionTask> tasks);

    int submitTask(@Param("id") Long id, @Param("actualValue") BigDecimal actualValue, @Param("status") String status);

    int approveTask(@Param("id") Long id, @Param("status") String status, @Param("approvedBy") String approvedBy);

    int rejectTask(@Param("id") Long id, @Param("status") String status, @Param("remark") String remark,
                    @Param("approvedBy") String approvedBy);

    int deleteByMonitoringId(Long monitoringId);

    int updateCollectorByInstitutionId(@Param("institutionId") Long institutionId,
                                      @Param("monitoringId") Long monitoringId,
                                      @Param("collectorName") String collectorName,
                                      @Param("collectorEmpNo") String collectorEmpNo,
                                      @Param("collectorUserId") Long collectorUserId);

    int updateFileKeyByCollector(@Param("monitoringId") Long monitoringId,
                                @Param("collectorUserId") Long collectorUserId,
                                @Param("fileKey") String fileKey);
}
