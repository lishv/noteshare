package com.note.controller.user;

import com.note.service.FollowService;
import com.note.vo.ActiveUserVO;
import com.note.vo.FollowStatsVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/follow")
@Tag(name = "关注管理", description = "用户关注与粉丝相关接口")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{targetUserId}")
    @Operation(summary = "关注作者", description = "关注指定的作者用户")
    public Result<Void> followUser(
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        followService.followUser(currentUserId, targetUserId);
        return Result.success("关注成功");
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "取消关注", description = "取消对指定作者的关注")
    public Result<Void> unfollowUser(
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        followService.unfollowUser(currentUserId, targetUserId);
        return Result.success("已取消关注");
    }

    @GetMapping("/{targetUserId}/status")
    @Operation(summary = "获取关注统计与状态", description = "获取目标作者的粉丝量、关注数及当前登录用户是否关注该作者")
    public Result<FollowStatsVO> getFollowStats(
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        FollowStatsVO stats = followService.getFollowStats(targetUserId, currentUserId);
        return Result.success(stats);
    }

    @GetMapping("/list")
    @Operation(summary = "获取我关注的作者列表", description = "分页获取当前登录用户所关注的所有作者")
    public Result<PageResult<ActiveUserVO>> getFollowedUsers(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        PageResult<ActiveUserVO> result = followService.getFollowedUsers(currentUserId, page, size);
        return Result.success(result);
    }
}
