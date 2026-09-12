package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "平台综合统计概览")
public class PlatformSummaryVO {
    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "总笔记数")
    private Long totalNotes;

    @Schema(description = "Markdown形式笔记数")
    private Long markdownNotes;

    @Schema(description = "原创创作笔记数(同markdownNotes)")
    private Long originalNotes;

    @Schema(description = "仅链接形式笔记数")
    private Long linkNotes;

    @Schema(description = "非链接形式笔记数")
    private Long nonLinkNotes;

    @Schema(description = "知识库总链接数")
    private Long totalMainLinks;

    @Schema(description = "公开笔记数")
    private Long publicNotes;

    @Schema(description = "私密笔记数")
    private Long privateNotes;

    @Schema(description = "总浏览量")
    private Long totalViews;

    @Schema(description = "总评论数")
    private Long totalComments;

    @Schema(description = "总收藏数")
    private Long totalFavorites;

    @Schema(description = "总关注关系数")
    private Long totalFollows;

    @Schema(description = "今日活跃用户数")
    private Long activeUsersToday;

    @Schema(description = "今日新增笔记数")
    private Long newNotesToday;
}
