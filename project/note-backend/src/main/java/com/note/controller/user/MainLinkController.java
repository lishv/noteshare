package com.note.controller.user;

import com.note.dto.MainLinkDTO;
import com.note.service.MainLinkService;
import com.note.vo.MainLinkVO;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/main-link")
@Tag(name = "总链接管理", description = "用户总链接的增删改查相关接口")
public class MainLinkController {

    private final MainLinkService mainLinkService;

    public MainLinkController(MainLinkService mainLinkService) {
        this.mainLinkService = mainLinkService;
    }

    @PostMapping
    @Operation(summary = "设置总链接", description = "设置或更新用户的主链接地址")
    public Result<MainLinkVO> setMainLink(@Valid @RequestBody MainLinkDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MainLinkVO result = mainLinkService.setMainLink(userId, dto.getMainUrl());
        return Result.success("总链接设置成功", result);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户总链接", description = "获取指定用户的主链接信息")
    public Result<MainLinkVO> getMainLink(@Parameter(description = "用户ID") @PathVariable Long userId) {
        MainLinkVO result = mainLinkService.getMainLinkByUserId(userId);
        return Result.success(result);
    }

    @GetMapping
    @Operation(summary = "获取我的总链接", description = "获取当前登录用户的主链接信息")
    public Result<MainLinkVO> getMyMainLink(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MainLinkVO result = mainLinkService.getMyMainLink(userId);
        return Result.success(result);
    }

    @DeleteMapping
    @Operation(summary = "删除总链接", description = "删除当前登录用户的主链接")
    public Result<Void> deleteMainLink(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        mainLinkService.deleteMainLink(userId);
        return Result.success("总链接删除成功", null);
    }
}
