package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.Institution;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface InstitutionMapper {

    @Select("SELECT * FROM institution WHERE id = #{id}")
    Institution selectById(Long id);

    @Select("SELECT * FROM institution WHERE system_id = #{systemId}")
    List<Institution> selectBySystemId(Long systemId);

    @Select("SELECT * FROM institution WHERE system_id = #{systemId} AND org_id = #{orgId}")
    Institution selectBySystemIdAndOrgId(@Param("systemId") Long systemId, @Param("orgId") String orgId);

    @Select("SELECT * FROM institution WHERE system_id = #{systemId} AND group_name = #{groupName}")
    List<Institution> selectBySystemIdAndGroupName(@Param("systemId") Long systemId, @Param("groupName") String groupName);

    @Insert("INSERT INTO institution (system_id, org_name, org_id, group_name, leader_name, leader_emp_no) " +
            "VALUES (#{systemId}, #{orgName}, #{orgId}, #{groupName}, #{leaderName}, #{leaderEmpNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Institution institution);

    @Insert("<script>" +
            "INSERT INTO institution (system_id, org_name, org_id, group_name, leader_name, leader_emp_no) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.systemId}, #{item.orgName}, #{item.orgId}, #{item.groupName}, #{item.leaderName}, #{item.leaderEmpNo})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<Institution> institutions);

    @Delete("DELETE FROM institution WHERE system_id = #{systemId}")
    int deleteBySystemId(Long systemId);

    @Select("SELECT DISTINCT group_name FROM institution WHERE system_id = #{systemId}")
    List<String> selectGroupNamesBySystemId(Long systemId);
}
