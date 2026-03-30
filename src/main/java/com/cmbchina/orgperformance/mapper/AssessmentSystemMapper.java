package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.AssessmentSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AssessmentSystemMapper {

    AssessmentSystem selectById(Long id);

    List<AssessmentSystem> selectAll();

    List<AssessmentSystem> selectList(@Param("name") String name, @Param("status") Integer status,
                                     @Param("offset") Integer offset, @Param("limit") Integer limit);

    int count(@Param("name") String name, @Param("status") Integer status);

    int insert(AssessmentSystem system);

    int update(AssessmentSystem system);

    int updateTemplateFileKey(@Param("id") Long id, @Param("templateFileKey") String templateFileKey);

    int deleteById(Long id);
}
