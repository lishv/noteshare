package com.note.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记草稿返回对象")
public class NoteDraftVO {

    @Schema(description = "笔记ID (若为新建草稿则为null)", example = "1")
    private Long noteId;

    @Schema(description = "笔记标题", example = "Redis缓存与草稿设计")
    private String title;

    @Schema(description = "笔记正文内容 (Markdown格式)")
    private String content;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "是否公开: 1-公开, 0-私密", example = "1")
    private Integer isPublic;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "最后保存时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime savedAt;
}
