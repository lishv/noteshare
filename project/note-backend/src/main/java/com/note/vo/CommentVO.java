package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论视图响应对象")
public class CommentVO {
    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "所属笔记ID", example = "10")
    private Long noteId;

    @Schema(description = "评论者用户ID", example = "5")
    private Long userId;

    @Schema(description = "评论者昵称", example = "张三")
    private String username;

    @Schema(description = "评论者头像URL")
    private String avatar;

    @Schema(description = "根评论ID")
    private Long parentId;

    @Schema(description = "被回复的目标用户ID")
    private Long replyToUserId;

    @Schema(description = "被回复的目标用户昵称", example = "李四")
    private String replyToUsername;

    @Schema(description = "被回复的具体评论ID")
    private Long replyToCommentId;

    @Schema(description = "评论正文内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "是否为当前登录用户的评论 (用于权限判断)")
    private Boolean isOwner;

    @Schema(description = "子回复列表")
    private List<CommentVO> replies;
}
