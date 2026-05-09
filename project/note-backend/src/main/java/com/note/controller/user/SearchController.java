package com.note.controller.user;

import com.note.service.SearchService;
import com.note.vo.SearchVO;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/search")
@Tag(name = "搜索管理", description = "全局搜索相关接口")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "全局搜索", description = "搜索用户和笔记内容")
    public Result<SearchVO> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        SearchVO result = searchService.search(keyword, page, size);
        return Result.success(result);
    }
}
