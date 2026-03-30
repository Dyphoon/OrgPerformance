package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.MonthlyMonitoring;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonthlyMonitoringMapper {

    MonthlyMonitoring selectById(Long id);

    List<MonthlyMonitoring> selectBySystemId(Long systemId);

    MonthlyMonitoring selectBySystemIdAndYearMonth(@Param("systemId") Long systemId,
                                                    @Param("year") Integer year,
                                                    @Param("month") Integer month);

    List<MonthlyMonitoring> selectList(@Param("systemId") Long systemId, @Param("status") String status,
                                        @Param("year") Integer year, @Param("month") Integer month,
                                        @Param("offset") Integer offset, @Param("limit") Integer limit);

    int count(@Param("systemId") Long systemId, @Param("status") String status);

    List<MonthlyMonitoring> selectByStatusAndProcessStatus(@Param("status") String status,
                                                            @Param("processStatus") String processStatus);

    int insert(MonthlyMonitoring monitoring);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateProcessStatus(@Param("id") Long id, @Param("processStatus") String processStatus,
                            @Param("processPercent") Integer processPercent, @Param("processMsg") String processMsg);

    int updateDeadline(@Param("id") Long id, @Param("deadline") LocalDateTime deadline);

    int deleteById(Long id);

    List<MonthlyMonitoring> selectAll();
}
