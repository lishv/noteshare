package com.note.controller.admin;

import com.note.service.DashboardService;
import com.note.vo.DashboardVO;
import com.note.vo.NoteVO;
import com.note.vo.Result;
import com.note.vo.TrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "管理端-仪表盘", description = "管理端仪表盘数据")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @Operation(summary = "概览统计", description = "获取仪表盘概览数据")
    public Result<DashboardVO> getOverview() {
        return Result.success(dashboardService.getOverview());
    }

    @GetMapping("/user-trend")
    @Operation(summary = "用户注册趋势", description = "获取用户注册趋势数据")
    public Result<List<TrendVO>> getUserTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(dashboardService.getUserTrend(days));
    }

    @GetMapping("/note-trend")
    @Operation(summary = "笔记发布趋势", description = "获取笔记发布趋势数据")
    public Result<List<TrendVO>> getNoteTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(dashboardService.getNoteTrend(days));
    }

    @GetMapping("/top-notes")
    @Operation(summary = "热门笔记", description = "获取浏览量最高的笔记")
    public Result<List<NoteVO>> getTopNotes(
            @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(dashboardService.getTopNotes(limit));
    }

    @GetMapping("/user-activity")
    @Operation(summary = "用户活跃度", description = "获取用户活跃度数据")
    public Result<List<TrendVO>> getUserActivity() {
        return Result.success(dashboardService.getUserActivity());
    }
}
