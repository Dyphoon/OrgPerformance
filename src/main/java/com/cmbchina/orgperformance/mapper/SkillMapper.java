package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.Skill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SkillMapper {

    Skill selectById(Long id);

    List<Skill> selectAll();

    List<Skill> selectByCategory(String category);

    List<Skill> selectBuiltIn();

    List<Skill> selectInstalledByUserId(@Param("userId") Long userId);

    int insert(Skill skill);

    int update(Skill skill);

    int deleteById(Long id);
}
