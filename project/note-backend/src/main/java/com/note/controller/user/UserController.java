package com.note.controller.user;

import com.note.dto.ChangePasswordDTO;
import com.note.dto.UpdateBioDTO;
import com.note.service.UserService;
import com.note.vo.PageResult;
import com.note.vo.ActiveUserVO;
import com.note.vo.Result;
import com.note.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/users")
@Tag(name = "用户管理", description = "用户信息管理相关接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO userVO = userService.getCurrentUser(userId);
        return Result.success(userVO);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户主页信息", description = "获取指定作者/用户的公开个人主页信息，包括笔记数、粉丝数、关注数及回关状态")
    public Result<ActiveUserVO> getUserProfile(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        ActiveUserVO vo = userService.getUserProfile(userId, currentUserId);
        return Result.success(vo);
    }

    @PutMapping("/bio")
    @Operation(summary = "修改个性签名", description = "修改当前登录用户的个性签名（最多30字）")
    public Result<UserVO> updateBio(@Valid @RequestBody UpdateBioDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO userVO = userService.updateBio(userId, dto.getBio());
        return Result.success("个性签名修改成功", userVO);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePassword(userId, dto);
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/active")
    @Operation(summary = "获取用户列表", description = "分页获取用户列表，登录时默认展示已关注作者，支持关键词搜索全量用户")
    public Result<PageResult<ActiveUserVO>> getActiveUsers(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        PageResult<ActiveUserVO> result = userService.getActiveUsers(currentUserId, page, size, keyword);
        return Result.success(result);
    }

    @GetMapping("/recommended")
    @Operation(summary = "获取推荐创作者", description = "获取系统推荐创作者列表，自动排除当前用户本人及已经关注的作者")
    public Result<List<ActiveUserVO>> getRecommendedUsers(
            @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "5") Integer limit,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        List<ActiveUserVO> list = userService.getRecommendedUsers(currentUserId, limit);
        return Result.success(list);
    }
}
