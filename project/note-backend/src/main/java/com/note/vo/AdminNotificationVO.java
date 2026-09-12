package com.note.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理端通知消息视图对象")
public class AdminNotificationVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "接收用户名")
    private String receiverUsername;

    @Schema(description = "接收用户头像")
    private String receiverAvatar;

    @Schema(description = "发送用户ID，系统消息为空")
    private Long senderId;

    @Schema(description = "发送用户名")
    private String senderUsername;

    @Schema(description = "发送用户头像")
    private String senderAvatar;

    @Schema(description = "通知类型")
    private String type;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "关联对象类型")
    private String targetType;

    @Schema(description = "关联对象ID")
    private Long targetId;

    @Schema(description = "关联笔记标题")
    private String targetTitle;

    @Schema(description = "是否已读: 0-未读, 1-已读")
    private Integer isRead;

    @Schema(description = "是否已移除: 0-正常, 1-已移除")
    private Integer isDeleted;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
