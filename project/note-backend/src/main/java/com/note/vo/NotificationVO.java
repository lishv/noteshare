package com.note.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "站内通知返回对象")
public class NotificationVO {

    @Schema(description = "通知ID", example = "1")
    private Long id;

    @Schema(description = "接收通知的用户ID", example = "1")
    private Long userId;

    @Schema(description = "发送者用户ID (系统通知为null)", example = "2")
    private Long senderId;

    @Schema(description = "发送者用户名")
    private String senderUsername;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "通知类型: COMMENT, FOLLOW, LIKE, SYSTEM", example = "COMMENT")
    private String type;

    @Schema(description = "通知简题", example = "张三 评论了你的笔记")
    private String title;

    @Schema(description = "通知详细内容")
    private String content;

    @Schema(description = "关联对象类型: NOTE, USER, COMMENT, SYSTEM", example = "NOTE")
    private String targetType;

    @Schema(description = "关联对象ID (如笔记ID)", example = "10")
    private Long targetId;

    @Schema(description = "是否已读: 0-未读, 1-已读", example = "0")
    private Integer isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
