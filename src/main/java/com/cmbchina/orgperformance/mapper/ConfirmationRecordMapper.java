package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.ConfirmationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConfirmationRecordMapper {

    ConfirmationRecord selectById(Long id);

    List<ConfirmationRecord> selectByMonitoringId(Long monitoringId);

    List<ConfirmationRecord> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                                   @Param("institutionId") Long institutionId);

    ConfirmationRecord selectByMonitoringIdAndUserId(@Param("monitoringId") Long monitoringId, @Param("userId") Long userId);

    int countByMonitoringId(Long monitoringId);

    int countByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId, @Param("status") String status);

    int insert(ConfirmationRecord record);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("remark") String remark);

    int deleteByMonitoringId(Long monitoringId);
}
