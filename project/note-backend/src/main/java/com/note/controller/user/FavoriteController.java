package com.note.controller.user;

import com.note.service.FavoriteService;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/favorites")
@Tag(name = "收藏管理", description = "笔记收藏相关接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{noteId}")
    @Operation(summary = "收藏笔记", description = "收藏指定笔记")
    public Result<Void> addFavorite(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.addFavorite(userId, noteId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "取消收藏", description = "取消收藏指定笔记")
    public Result<Void> removeFavorite(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.removeFavorite(userId, noteId);
        return Result.success("取消收藏成功");
    }

    @GetMapping
    @Operation(summary = "我的收藏列表", description = "分页获取当前用户的收藏笔记列表")
    public Result<PageResult<NoteVO>> listMyFavorites(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.listMyFavorites(userId, page, size));
    }

    @GetMapping("/check/{noteId}")
    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否已收藏指定笔记")
    public Result<Boolean> checkFavorite(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.isFavorited(userId, noteId));
    }
}
