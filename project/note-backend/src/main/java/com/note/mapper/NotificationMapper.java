package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.Notification;
import com.note.vo.AdminNotificationVO;
import com.note.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("<script>" +
            "SELECT n.id, n.user_id, n.sender_id, u.username as senderUsername, u.avatar as senderAvatar, " +
            "n.type, n.title, n.content, n.target_type, n.target_id, n.is_read, n.created_at " +
            "FROM t_notification n " +
            "LEFT JOIN t_user u ON n.sender_id = u.id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = 0 " +
            "<if test=\"type != null and type != ''\">" +
            "  AND n.type = #{type} " +
            "</if>" +
            "ORDER BY n.created_at DESC" +
            "</script>")
    IPage<NotificationVO> selectNotificationPage(Page<NotificationVO> page,
                                                 @Param("userId") Long userId,
                                                 @Param("type") String type);

    @Select("SELECT COUNT(1) FROM t_notification WHERE user_id = #{userId} AND is_deleted = 0 AND is_read = 0")
    Long countUnread(@Param("userId") Long userId);

    @Update("UPDATE t_notification SET is_deleted = 1 WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int logicalDeleteForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_notification SET is_read = 1 WHERE user_id = #{userId} AND is_deleted = 0 AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);

    IPage<AdminNotificationVO> selectAdminNotificationPage(Page<AdminNotificationVO> page,
                                                            @Param("keyword") String keyword,
                                                            @Param("userId") Long userId,
                                                            @Param("type") String type,
                                                            @Param("isRead") Integer isRead,
                                                            @Param("isDeleted") Integer isDeleted,
                                                            @Param("startTime") String startTime,
                                                            @Param("endTime") String endTime);

    @Update("UPDATE t_notification SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int logicalDeleteByAdmin(@Param("id") Long id);

    @Update("UPDATE t_notification SET is_deleted = 0 WHERE id = #{id} AND is_deleted = 1")
    int restoreByAdmin(@Param("id") Long id);
}
