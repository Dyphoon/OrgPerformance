package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.MonthlyIndicatorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MonthlyIndicatorDataMapper {

    MonthlyIndicatorData selectById(Long id);

    List<MonthlyIndicatorData> selectByMonitoringId(Long monitoringId);

    List<MonthlyIndicatorData> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                                     @Param("institutionId") Long institutionId);

    List<MonthlyIndicatorData> selectByMonitoringIdAndIndicatorId(@Param("monitoringId") Long monitoringId,
                                                                   @Param("indicatorId") Long indicatorId);

    List<MonthlyIndicatorData> selectDetailByInstitution(@Param("monitoringId") Long monitoringId,
                                                        @Param("institutionId") Long institutionId);

    String selectFileKeyByMonitoringAndInstitution(@Param("monitoringId") Long monitoringId,
                                                    @Param("institutionId") Long institutionId);

    int batchInsert(@Param("list") List<MonthlyIndicatorData> dataList);

    int deleteByMonitoringId(Long monitoringId);

    int deleteByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId, @Param("institutionId") Long institutionId);
}
