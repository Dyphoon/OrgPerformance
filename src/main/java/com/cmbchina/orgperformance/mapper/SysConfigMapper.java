package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysConfigMapper {

    SysConfig selectById(Long id);

    SysConfig selectByKey(String configKey);

    List<SysConfig> selectAll();

    int insert(SysConfig config);

    int update(SysConfig config);

    int deleteById(Long id);
}
