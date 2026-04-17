package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.UserSkill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserSkillMapper {

    UserSkill selectByUserIdAndSkillId(@Param("userId") Long userId, @Param("skillId") Long skillId);

    List<UserSkill> selectByUserId(@Param("userId") Long userId);

    int insert(UserSkill userSkill);

    int update(UserSkill userSkill);

    int deleteByUserIdAndSkillId(@Param("userId") Long userId, @Param("skillId") Long skillId);
}
