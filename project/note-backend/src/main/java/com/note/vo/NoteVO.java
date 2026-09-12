package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记信息响应数据")
public class NoteVO {
    @Schema(description = "笔记ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "笔记标题", example = "Java基础笔记")
    private String title;

    @Schema(description = "笔记类型: 1-链接笔记, 2-创作笔记", example = "1")
    private Integer noteType;

    @Schema(description = "链接或位置", example = "https://example.com/note")
    private String locationOrUrl;

    @Schema(description = "笔记正文内容 (Markdown格式)")
    private String content;

    @Schema(description = "笔记摘要")
    private String summary;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "浏览量", example = "10")
    private Long viewCount;

    @Schema(description = "是否已删除: 0-正常, 1-已删除")
    private Integer isDeleted;

    @Schema(description = "是否置顶: 0-否, 1-是")
    private Integer isPinned;

    @Schema(description = "是否公开: 1-公开, 0-私密", example = "1")
    private Integer isPublic;

    @Schema(description = "收藏数")
    private Long favoriteCount;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "当前登录用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "当前登录用户是否已收藏")
    private Boolean isFavorited;

    @Schema(description = "标签名称列表")
    private List<String> tags;

    @Schema(description = "删除时间 (回收站状态下有效)")
    private LocalDateTime deletedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
