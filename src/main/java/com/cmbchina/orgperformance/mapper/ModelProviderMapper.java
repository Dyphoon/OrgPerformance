package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.ModelProvider;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelProviderMapper {

    @Select("SELECT * FROM model_provider ORDER BY sort_order ASC, id ASC")
    List<ModelProvider> findAll();

    @Select("SELECT * FROM model_provider WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<ModelProvider> findAllActive();

    @Select("SELECT * FROM model_provider WHERE id = #{id}")
    ModelProvider findById(Long id);

    @Select("SELECT * FROM model_provider WHERE code = #{code}")
    ModelProvider findByCode(String code);

    @Insert("INSERT INTO model_provider (name, code, base_url, api_key, model_name, model_type, max_tokens, temperature, sort_order, status, created_at, updated_at) " +
            "VALUES (#{name}, #{code}, #{baseUrl}, #{apiKey}, #{modelName}, #{modelType}, #{maxTokens}, #{temperature}, #{sortOrder}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelProvider modelProvider);

    @Update("UPDATE model_provider SET name = #{name}, code = #{code}, base_url = #{baseUrl}, api_key = #{apiKey}, " +
            "model_name = #{modelName}, model_type = #{modelType}, max_tokens = #{maxTokens}, temperature = #{temperature}, " +
            "sort_order = #{sortOrder}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int update(ModelProvider modelProvider);

    @Delete("DELETE FROM model_provider WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM model_provider")
    int count();
}