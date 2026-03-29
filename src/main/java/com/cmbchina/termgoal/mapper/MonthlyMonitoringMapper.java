package com.cmbchina.termgoal.mapper;

import com.cmbchina.termgoal.entity.MonthlyMonitoring;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MonthlyMonitoringMapper {

    @Select("SELECT * FROM monthly_monitoring WHERE id = #{id}")
    MonthlyMonitoring selectById(Long id);

    @Select("SELECT * FROM monthly_monitoring WHERE system_id = #{systemId} ORDER BY year DESC, month DESC")
    List<MonthlyMonitoring> selectBySystemId(Long systemId);

    @Select("SELECT * FROM monthly_monitoring WHERE system_id = #{systemId} AND year = #{year} AND month = #{month}")
    MonthlyMonitoring selectBySystemIdAndYearMonth(@Param("systemId") Long systemId,
                                                    @Param("year") Integer year,
                                                    @Param("month") Integer month);

    @Select("<script>" +
            "SELECT * FROM monthly_monitoring WHERE 1=1" +
            "<if test='systemId != null'> AND system_id = #{systemId}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='year != null'> AND year = #{year}</if>" +
            "<if test='month != null'> AND month = #{month}</if>" +
            " ORDER BY created_at DESC" +
            "<if test='offset != null'> LIMIT #{offset}, #{limit}</if>" +
            "</script>")
    List<MonthlyMonitoring> selectList(@Param("systemId") Long systemId, @Param("status") String status,
                                        @Param("year") Integer year, @Param("month") Integer month,
                                        @Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("<script>SELECT COUNT(*) FROM monthly_monitoring WHERE 1=1" +
            "<if test='systemId != null'> AND system_id = #{systemId}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "</script>")
    int count(@Param("systemId") Long systemId, @Param("status") String status);

    @Select("SELECT * FROM monthly_monitoring WHERE status = #{status} AND process_status = #{processStatus}")
    List<MonthlyMonitoring> selectByStatusAndProcessStatus(@Param("status") String status,
                                                            @Param("processStatus") String processStatus);

    @Insert("INSERT INTO monthly_monitoring (system_id, year, month, status, deadline, approval_required, created_by) " +
            "VALUES (#{systemId}, #{year}, #{month}, #{status}, #{deadline}, #{approvalRequired}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MonthlyMonitoring monitoring);

    @Update("UPDATE monthly_monitoring SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE monthly_monitoring SET process_status=#{processStatus}, process_percent=#{processPercent}, " +
            "process_msg=#{processMsg}, updated_at=NOW() WHERE id=#{id}")
    int updateProcessStatus(@Param("id") Long id, @Param("processStatus") String processStatus,
                            @Param("processPercent") Integer processPercent, @Param("processMsg") String processMsg);

    @Update("UPDATE monthly_monitoring SET deadline=#{deadline} WHERE id=#{id}")
    int updateDeadline(@Param("id") Long id, @Param("deadline") java.time.LocalDateTime deadline);

    @Delete("DELETE FROM monthly_monitoring WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM monthly_monitoring ORDER BY created_at DESC")
    List<MonthlyMonitoring> selectAll();
}
