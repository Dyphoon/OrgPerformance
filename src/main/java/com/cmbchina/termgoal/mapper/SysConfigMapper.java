package com.cmbchina.termgoal.mapper;

import com.cmbchina.termgoal.entity.SysConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysConfigMapper {

    @Select("SELECT * FROM sys_config WHERE id = #{id}")
    SysConfig selectById(Long id);

    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey}")
    SysConfig selectByKey(String configKey);

    @Select("SELECT * FROM sys_config ORDER BY id")
    List<SysConfig> selectAll();

    @Insert("INSERT INTO sys_config (config_key, config_value, description) VALUES (#{configKey}, #{configValue}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysConfig config);

    @Update("UPDATE sys_config SET config_value=#{configValue}, description=#{description} WHERE config_key=#{configKey}")
    int update(SysConfig config);

    @Delete("DELETE FROM sys_config WHERE id = #{id}")
    int deleteById(Long id);
}
