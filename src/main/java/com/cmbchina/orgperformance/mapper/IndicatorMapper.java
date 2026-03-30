package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.Indicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface IndicatorMapper {

    Indicator selectById(Long id);

    List<Indicator> selectBySystemId(Long systemId);

    List<Indicator> selectBySystemIdAndDimension(@Param("systemId") Long systemId, @Param("dimension") String dimension);

    List<String> selectDimensionsBySystemId(Long systemId);

    List<String> selectCategoriesByDimension(@Param("systemId") Long systemId, @Param("dimension") String dimension);

    int batchInsert(@Param("list") List<Indicator> indicators);

    int deleteBySystemId(Long systemId);
}
