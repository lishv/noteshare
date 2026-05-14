package com.note.controller.user;

import com.note.entity.Tag;
import com.note.service.TagService;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "笔记标签相关接口")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "获取我的标签列表", description = "获取当前用户的所有标签")
    public Result<List<Tag>> listMyTags(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(tagService.getUserTags(userId));
    }

    @PostMapping
    @Operation(summary = "创建标签", description = "创建新标签，已存在则返回已有的")
    public Result<Tag> createTag(@RequestParam String name, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(tagService.createOrGetTag(userId, name));
    }

    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签", description = "删除指定标签及其关联关系")
    public Result<Void> deleteTag(@PathVariable Long tagId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        tagService.deleteTag(tagId, userId);
        return Result.success("删除成功");
    }
}
