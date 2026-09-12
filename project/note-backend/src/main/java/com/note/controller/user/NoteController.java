package com.note.controller.user;

import com.note.dto.NoteDTO;
import com.note.dto.NoteDraftDTO;
import com.note.enums.ErrorCode;
import com.note.service.NoteService;
import com.note.vo.NoteDraftVO;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/notes")
@Tag(name = "笔记管理", description = "笔记的增删改查及草稿相关接口")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @Operation(summary = "创建笔记", description = "创建新的笔记内容")
    public Result<NoteVO> createNote(@Valid @RequestBody NoteDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteVO result = noteService.createNote(userId, dto);
        return Result.success("笔记创建成功", result);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户笔记列表", description = "分页获取指定用户的笔记列表，支持关键词模糊搜索和时间范围")
    public Result<PageResult<NoteVO>> getUserNotes(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "笔记类型: 1-链接, 2-创作") @RequestParam(required = false) Integer noteType,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        PageResult<NoteVO> result = noteService.getUserNotes(userId, currentUserId, page, size, keyword, noteType, startDate, endDate);
        return Result.success(result);
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记详情", description = "根据ID获取笔记详细信息")
    public Result<NoteVO> getNote(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        NoteVO result = noteService.getNoteById(noteId, currentUserId);
        return Result.success(result);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新指定笔记的内容")
    public Result<NoteVO> updateNote(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            @Valid @RequestBody NoteDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteVO result = noteService.updateNote(noteId, userId, dto);
        return Result.success("笔记更新成功", result);
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "删除指定的笔记")
    public Result<Void> deleteNote(@Parameter(description = "笔记ID") @PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.deleteNote(noteId, userId);
        return Result.success("笔记删除成功", null);
    }

    @PostMapping("/{noteId}/view")
    @Operation(summary = "增加浏览量", description = "增加指定笔记的浏览量计数")
    public Result<Void> incrementViewCount(@Parameter(description = "笔记ID") @PathVariable Long noteId) {
        noteService.incrementViewCount(noteId);
        return Result.success(null);
    }

    @PutMapping("/{noteId}/pin")
    @Operation(summary = "置顶笔记", description = "将指定笔记设为置顶")
    public Result<Void> pinNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.pinNote(noteId, userId);
        return Result.success("置顶成功");
    }

    @PutMapping("/{noteId}/unpin")
    @Operation(summary = "取消置顶", description = "取消指定笔记的置顶")
    public Result<Void> unpinNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.unpinNote(noteId, userId);
        return Result.success("取消置顶成功");
    }

    @PostMapping("/draft")
    @Operation(summary = "保存/更新笔记草稿", description = "静默自动保存草稿至 Redis 缓存")
    public Result<NoteDraftVO> saveDraft(@RequestBody NoteDraftDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteDraftVO result = noteService.saveDraft(userId, dto);
        return Result.success("草稿保存成功", result);
    }

    @GetMapping("/draft")
    @Operation(summary = "获取笔记草稿", description = "从 Redis 获取用户的草稿（noteId为空时获取新建草稿）")
    public Result<NoteDraftVO> getDraft(
            @Parameter(description = "笔记ID (新建草稿不传)") @RequestParam(required = false) Long noteId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteDraftVO result = noteService.getDraft(userId, noteId);
        return Result.success(result);
    }

    @DeleteMapping("/draft")
    @Operation(summary = "删除/丢弃笔记草稿", description = "从 Redis 清除指定的草稿缓存")
    public Result<Void> deleteDraft(
            @Parameter(description = "笔记ID (新建草稿不传)") @RequestParam(required = false) Long noteId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.deleteDraft(userId, noteId);
        return Result.success("草稿已清除", null);
    }

    // ==================== 笔记回收站 ====================

    @GetMapping("/recycle-bin")
    @Operation(summary = "获取回收站笔记列表", description = "分页获取当前登录用户已软删除的笔记")
    public Result<PageResult<NoteVO>> getRecycleBinNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        PageResult<NoteVO> result = noteService.getRecycleBinNotes(userId, page, size);
        return Result.success(result);
    }

    @PutMapping("/recycle-bin/{noteId}/restore")
    @Operation(summary = "恢复回收站笔记", description = "从回收站恢复已删除的笔记")
    public Result<Void> restoreNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        noteService.restoreNote(noteId, userId);
        return Result.success("笔记恢复成功", null);
    }

    @DeleteMapping("/recycle-bin/{noteId}")
    @Operation(summary = "彻底删除笔记", description = "从回收站彻底物理销毁笔记及其关联数据")
    public Result<Void> hardDeleteNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        noteService.hardDeleteNote(noteId, userId);
        return Result.success("笔记已彻底销毁", null);
    }

    @DeleteMapping("/recycle-bin/clear")
    @Operation(summary = "一键清空回收站", description = "彻底清空当前登录用户回收站内的所有笔记")
    public Result<Void> clearRecycleBin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        noteService.clearRecycleBin(userId);
        return Result.success("回收站已清空", null);
    }

    // ==================== 关注动态流 ====================

    @GetMapping("/feed")
    @Operation(summary = "获取关注动态流", description = "分页获取当前用户关注的所有创作者发布的最新公开笔记")
    public Result<PageResult<NoteVO>> getFollowingFeed(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录查看关注动态");
        }
        PageResult<NoteVO> result = noteService.getFollowingFeed(userId, page, size);
        return Result.success(result);
    }

    // ==================== 全站热度榜与推荐 ====================

    @GetMapping("/trending")
    @Operation(summary = "获取全站热度榜", description = "按热度算法获取全站热门笔记排行 (阅读*1 + 点赞*3 + 收藏*5 + 评论*2)")
    public Result<java.util.List<NoteVO>> getTrendingNotes(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit) {
        java.util.List<NoteVO> result = noteService.getTrendingNotes(limit);
        return Result.success(result);
    }

    @GetMapping("/recommend")
    @Operation(summary = "获取公共推荐笔记", description = "基于浏览量、点赞量、收藏量和评论量热度推荐公开笔记，支持标签筛选与分页，默认一次6条")
    public Result<PageResult<NoteVO>> getRecommendedNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "6") Integer size,
            @Parameter(description = "标签筛选") @RequestParam(required = false) String tag,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        PageResult<NoteVO> result = noteService.getRecommendedNotes(page, size, currentUserId, tag);
        return Result.success(result);
    }

    // ==================== 点赞机制 ====================

    @PostMapping("/{noteId}/like")
    @Operation(summary = "点赞笔记", description = "对指定笔记进行点赞")
    public Result<Void> likeNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录再点赞");
        }
        noteService.likeNote(noteId, userId);
        return Result.success("点赞成功", null);
    }

    @DeleteMapping("/{noteId}/like")
    @Operation(summary = "取消点赞", description = "取消对指定笔记的点赞")
    public Result<Void> unlikeNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        noteService.unlikeNote(noteId, userId);
        return Result.success("已取消点赞", null);
    }

    @GetMapping("/{noteId}/like")
    @Operation(summary = "获取点赞状态", description = "检查当前登录用户是否已点赞该笔记")
    public Result<Boolean> checkLiked(@PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = noteService.checkLiked(noteId, userId);
        return Result.success(liked);
    }
}
