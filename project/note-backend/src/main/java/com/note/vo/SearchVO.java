package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "搜索响应数据")
public class SearchVO {
    @Schema(description = "总记录数", example = "20")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "10")
    private Integer size;

    @Schema(description = "用户列表")
    private List<ActiveUserVO> users;

    @Schema(description = "笔记列表")
    private List<NoteVO> notes;
}
