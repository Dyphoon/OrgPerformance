package com.cmbchina.orgperformance.mapper;

import com.cmbchina.orgperformance.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NotificationMapper {

    Notification selectById(Long id);

    List<Notification> selectByUserId(Long userId);

    int countUnreadByUserId(Long userId);

    int insert(Notification notification);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateSendResult(@Param("id") Long id, @Param("sendResult") String sendResult);

    int updateSendFailed(@Param("id") Long id, @Param("sendResult") String sendResult);

    int deleteById(Long id);
}
