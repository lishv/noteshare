package com.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long senderId;

    /**
     * 通知类型: COMMENT (评论回复), FOLLOW (新增粉丝), LIKE (点赞通知), SYSTEM (系统通知)
     */
    private String type;

    private String title;

    private String content;

    /**
     * 关联业务类型: NOTE, USER, COMMENT, SYSTEM
     */
    private String targetType;

    private Long targetId;

    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;

    /**
     * 是否已移除: 0-正常, 1-已移除
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime createdAt;
}
