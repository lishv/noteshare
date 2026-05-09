package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "仪表盘概览数据")
public class DashboardVO {
    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "总笔记数")
    private Long totalNotes;

    @Schema(description = "总浏览量")
    private Long totalViews;

    @Schema(description = "今日活跃用户数")
    private Long activeUsersToday;

    @Schema(description = "今日新增笔记数")
    private Long newNotesToday;
}
