package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser selectById(Long id);

    SysUser selectByUsername(String username);

    SysUser selectByEmpNo(String empNo);

    List<SysUser> selectAllActive();

    List<SysUser> selectList(SysUser user);

    int insert(SysUser user);

    int update(SysUser user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int deleteById(Long id);

    List<Long> selectRoleIdsByUserId(Long userId);

    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    int deleteUserRoles(Long userId);

    List<SysUser> selectByRoleCode(String roleCode);
}
