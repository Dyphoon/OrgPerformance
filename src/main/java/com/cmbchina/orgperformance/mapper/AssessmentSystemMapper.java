package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.AssessmentSystem;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AssessmentSystemMapper {

    @Select("SELECT * FROM assessment_system WHERE id = #{id}")
    AssessmentSystem selectById(Long id);

    @Select("SELECT * FROM assessment_system WHERE status = 1 ORDER BY created_at DESC")
    List<AssessmentSystem> selectAll();

    @Select("<script>" +
            "SELECT * FROM assessment_system WHERE 1=1" +
            "<if test='name != null'> AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            " ORDER BY created_at DESC" +
            "<if test='offset != null'> LIMIT #{offset}, #{limit}</if>" +
            "</script>")
    List<AssessmentSystem> selectList(@Param("name") String name, @Param("status") Integer status,
                                       @Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("<script>SELECT COUNT(*) FROM assessment_system WHERE 1=1" +
            "<if test='name != null'> AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "</script>")
    int count(@Param("name") String name, @Param("status") Integer status);

    @Insert("INSERT INTO assessment_system (name, description, template_file_key, need_approval, status, created_by) " +
            "VALUES (#{name}, #{description}, #{templateFileKey}, #{needApproval}, #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssessmentSystem system);

    @Update("UPDATE assessment_system SET name=#{name}, description=#{description}, " +
            "need_approval=#{needApproval}, status=#{status} WHERE id=#{id}")
    int update(AssessmentSystem system);

    @Update("UPDATE assessment_system SET template_file_key=#{templateFileKey} WHERE id=#{id}")
    int updateTemplateFileKey(@Param("id") Long id, @Param("templateFileKey") String templateFileKey);

    @Delete("DELETE FROM assessment_system WHERE id = #{id}")
    int deleteById(Long id);
}
