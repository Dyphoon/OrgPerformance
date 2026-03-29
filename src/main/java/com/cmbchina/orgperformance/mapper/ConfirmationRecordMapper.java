package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.ConfirmationRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ConfirmationRecordMapper {

    @Select("SELECT * FROM confirmation_record WHERE id = #{id}")
    ConfirmationRecord selectById(Long id);

    @Select("SELECT * FROM confirmation_record WHERE monitoring_id = #{monitoringId}")
    List<ConfirmationRecord> selectByMonitoringId(Long monitoringId);

    @Select("SELECT * FROM confirmation_record WHERE monitoring_id = #{monitoringId} AND institution_id = #{institutionId}")
    List<ConfirmationRecord> selectByMonitoringIdAndInstitutionId(@Param("monitoringId") Long monitoringId,
                                                                   @Param("institutionId") Long institutionId);

    @Select("SELECT * FROM confirmation_record WHERE monitoring_id = #{monitoringId} AND user_id = #{userId}")
    ConfirmationRecord selectByMonitoringIdAndUserId(@Param("monitoringId") Long monitoringId, @Param("userId") Long userId);

    @Insert("INSERT INTO confirmation_record (monitoring_id, institution_id, user_id, status) " +
            "VALUES (#{monitoringId}, #{institutionId}, #{userId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConfirmationRecord record);

    @Update("UPDATE confirmation_record SET status=#{status}, confirmed_at=NOW(), remark=#{remark} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("remark") String remark);

    @Select("SELECT COUNT(*) FROM confirmation_record WHERE monitoring_id = #{monitoringId}")
    int countByMonitoringId(Long monitoringId);

    @Select("SELECT COUNT(*) FROM confirmation_record WHERE monitoring_id = #{monitoringId} AND status = #{status}")
    int countByMonitoringIdAndStatus(@Param("monitoringId") Long monitoringId, @Param("status") String status);

    @Delete("DELETE FROM confirmation_record WHERE monitoring_id = #{monitoringId}")
    int deleteByMonitoringId(Long monitoringId);
}
