package com.cmbchina.termgoal.mapper;

import com.cmbchina.termgoal.entity.Notification;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Select("SELECT * FROM notification WHERE id = #{id}")
    Notification selectById(Long id);

    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Notification> selectByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND status = 'pending'")
    int countUnreadByUserId(Long userId);

    @Insert("INSERT INTO notification (user_id, title, content, type, status) " +
            "VALUES (#{userId}, #{title}, #{content}, #{type}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Update("UPDATE notification SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE notification SET status='sent', sent_at=NOW(), send_result=#{sendResult} WHERE id=#{id}")
    int updateSendResult(@Param("id") Long id, @Param("sendResult") String sendResult);

    @Update("UPDATE notification SET status='failed', send_result=#{sendResult} WHERE id=#{id}")
    int updateSendFailed(@Param("id") Long id, @Param("sendResult") String sendResult);

    @Delete("DELETE FROM notification WHERE id = #{id}")
    int deleteById(Long id);
}
