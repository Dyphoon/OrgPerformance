package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysRoleMapper {

    SysRole selectById(Long id);

    SysRole selectByCode(String roleCode);

    List<SysRole> selectAll();

    int insert(SysRole role);
}
