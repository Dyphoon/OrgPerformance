package com.cmbchina.termgoal.mapper;

import com.cmbchina.termgoal.entity.SysRole;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysRoleMapper {

    @Select("SELECT * FROM sys_role WHERE id = #{id}")
    SysRole selectById(Long id);

    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode}")
    SysRole selectByCode(String roleCode);

    @Insert("INSERT INTO sys_role (role_code, role_name, description, created_at) " +
            "VALUES (#{roleCode}, #{roleName}, #{description}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRole role);

    @Select("SELECT * FROM sys_role")
    java.util.List<SysRole> selectAll();
}
