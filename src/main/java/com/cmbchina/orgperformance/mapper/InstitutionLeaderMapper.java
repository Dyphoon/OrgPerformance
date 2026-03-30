package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.InstitutionLeader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface InstitutionLeaderMapper {

    InstitutionLeader selectById(Long id);

    List<InstitutionLeader> selectByInstitutionId(Long institutionId);

    List<InstitutionLeader> selectByUserId(Long userId);

    InstitutionLeader selectByInstitutionIdAndUserId(@Param("institutionId") Long institutionId, @Param("userId") Long userId);

    int countConfirmedByInstitutionId(Long institutionId);

    int countByInstitutionId(Long institutionId);

    int insert(InstitutionLeader leader);

    int batchInsert(@Param("list") List<InstitutionLeader> leaders);

    int updateConfirmed(@Param("institutionId") Long institutionId, @Param("userId") Long userId,
                        @Param("confirmed") Boolean confirmed);

    int deleteByInstitutionId(Long institutionId);
}
