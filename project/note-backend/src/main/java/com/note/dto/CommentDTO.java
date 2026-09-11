package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发表/回复评论请求")
public class CommentDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    @Schema(description = "评论正文内容", example = "这是一条很有用的笔记，感谢分享！")
    private String content;

    @Schema(description = "根评论ID (若为顶级评论则传null或不传)", example = "1")
    private Long parentId;

    @Schema(description = "被回复的目标用户ID (可选)", example = "2")
    private Long replyToUserId;

    @Schema(description = "被回复的具体评论ID (可选)", example = "10")
    private Long replyToCommentId;
}
