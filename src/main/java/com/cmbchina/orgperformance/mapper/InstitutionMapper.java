package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.Institution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface InstitutionMapper {

    Institution selectById(Long id);

    List<Institution> selectBySystemId(Long systemId);

    Institution selectBySystemIdAndOrgId(@Param("systemId") Long systemId, @Param("orgId") String orgId);

    List<Institution> selectBySystemIdAndGroupName(@Param("systemId") Long systemId, @Param("groupName") String groupName);

    List<String> selectGroupNamesBySystemId(Long systemId);

    int insert(Institution institution);

    int batchInsert(@Param("list") List<Institution> institutions);

    int deleteBySystemId(Long systemId);
}
