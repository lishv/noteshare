package com.note.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理端评论视图对象")
public class AdminCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "笔记标题")
    private String noteTitle;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "评论用户名")
    private String username;

    @Schema(description = "评论用户头像")
    private String avatar;

    @Schema(description = "根评论ID")
    private Long parentId;

    @Schema(description = "回复目标用户ID")
    private Long replyToUserId;

    @Schema(description = "回复目标用户名")
    private String replyToUsername;

    @Schema(description = "回复目标评论ID")
    private Long replyToCommentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "是否已删除: 0-正常, 1-已删除")
    private Integer isDeleted;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
