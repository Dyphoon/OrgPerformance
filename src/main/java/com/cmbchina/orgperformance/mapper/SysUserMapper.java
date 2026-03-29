package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.SysUser;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser selectById(Long id);

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE emp_no = #{empNo}")
    SysUser selectByEmpNo(String empNo);

    @Select("SELECT * FROM sys_user WHERE status = 1")
    List<SysUser> selectAllActive();

    @Select("<script>" +
            "SELECT * FROM sys_user WHERE 1=1" +
            "<if test='name != null'> AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='empNo != null'> AND emp_no = #{empNo}</if>" +
            "</script>")
    List<SysUser> selectList(SysUser user);

    @Insert("INSERT INTO sys_user (username, password, name, emp_no, email, phone, status) " +
            "VALUES (#{username}, #{password}, #{name}, #{empNo}, #{email}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("UPDATE sys_user SET name=#{name}, email=#{email}, phone=#{phone}, status=#{status} WHERE id=#{id}")
    int update(SysUser user);

    @Update("UPDATE sys_user SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    @Insert("<script>" +
            "INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach>" +
            "</script>")
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(Long userId);

    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE r.role_code = #{roleCode} AND u.status = 1")
    List<SysUser> selectByRoleCode(String roleCode);
}
