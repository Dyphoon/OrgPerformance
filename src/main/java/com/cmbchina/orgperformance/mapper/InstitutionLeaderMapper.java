package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.InstitutionLeader;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface InstitutionLeaderMapper {

    @Select("SELECT * FROM institution_leader WHERE id = #{id}")
    InstitutionLeader selectById(Long id);

    @Select("SELECT * FROM institution_leader WHERE institution_id = #{institutionId}")
    List<InstitutionLeader> selectByInstitutionId(Long institutionId);

    @Select("SELECT * FROM institution_leader WHERE user_id = #{userId}")
    List<InstitutionLeader> selectByUserId(Long userId);

    @Select("SELECT * FROM institution_leader WHERE institution_id = #{institutionId} AND user_id = #{userId}")
    InstitutionLeader selectByInstitutionIdAndUserId(@Param("institutionId") Long institutionId, @Param("userId") Long userId);

    @Insert("INSERT INTO institution_leader (institution_id, user_id, confirmed) VALUES (#{institutionId}, #{userId}, #{confirmed})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstitutionLeader leader);

    @Insert("<script>" +
            "INSERT INTO institution_leader (institution_id, user_id, confirmed) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.institutionId}, #{item.userId}, #{item.confirmed})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<InstitutionLeader> leaders);

    @Update("UPDATE institution_leader SET confirmed=#{confirmed}, confirmed_at=NOW() " +
            "WHERE institution_id=#{institutionId} AND user_id=#{userId}")
    int updateConfirmed(@Param("institutionId") Long institutionId, @Param("userId") Long userId,
                        @Param("confirmed") Boolean confirmed);

    @Delete("DELETE FROM institution_leader WHERE institution_id = #{institutionId}")
    int deleteByInstitutionId(Long institutionId);

    @Select("SELECT COUNT(*) FROM institution_leader WHERE institution_id = #{institutionId} AND confirmed = 1")
    int countConfirmedByInstitutionId(Long institutionId);

    @Select("SELECT COUNT(*) FROM institution_leader WHERE institution_id = #{institutionId}")
    int countByInstitutionId(Long institutionId);
}
