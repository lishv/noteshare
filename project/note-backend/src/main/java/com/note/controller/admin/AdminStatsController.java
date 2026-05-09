package com.note.controller.admin;

import com.note.service.AdminStatsService;
import com.note.vo.DistributionVO;
import com.note.vo.NoteVO;
import com.note.vo.Result;
import com.note.vo.TrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/stats")
@Tag(name = "管理端-数据统计", description = "管理端数据统计接口")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/user-registration-trend")
    @Operation(summary = "用户注册趋势", description = "获取用户注册趋势数据")
    public Result<List<TrendVO>> getRegistrationTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(adminStatsService.getRegistrationTrend(days));
    }

    @GetMapping("/user-activity")
    @Operation(summary = "用户活跃度统计", description = "获取用户活跃度数据")
    public Result<List<TrendVO>> getActivityStats(
            @Parameter(description = "周期: daily/weekly/monthly") @RequestParam(defaultValue = "daily") String period) {
        return Result.success(adminStatsService.getActivityStats(period));
    }

    @GetMapping("/user-note-distribution")
    @Operation(summary = "用户笔记分布", description = "获取用户笔记数量分布")
    public Result<List<DistributionVO>> getUserNoteDistribution() {
        return Result.success(adminStatsService.getUserNoteDistribution());
    }

    @GetMapping("/note-publish-trend")
    @Operation(summary = "笔记发布趋势", description = "获取笔记发布趋势数据")
    public Result<List<TrendVO>> getPublishTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(adminStatsService.getPublishTrend(days));
    }

    @GetMapping("/note-view-ranking")
    @Operation(summary = "笔记浏览排行", description = "获取笔记浏览量排行")
    public Result<List<NoteVO>> getViewRanking(
            @Parameter(description = "数量") @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(adminStatsService.getViewRanking(limit));
    }

    @GetMapping("/note-link-type")
    @Operation(summary = "链接类型统计", description = "获取笔记链接类型比例")
    public Result<List<DistributionVO>> getLinkTypeStats() {
        return Result.success(adminStatsService.getLinkTypeStats());
    }

    @GetMapping("/platform-trend")
    @Operation(summary = "平台数据趋势", description = "获取平台整体数据趋势")
    public Result<List<TrendVO>> getPlatformTrends(
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(adminStatsService.getPlatformTrends(days));
    }
}
