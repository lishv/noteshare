package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_note_item")
public class NoteItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    private Integer noteType;

    private String locationOrUrl;

    private String content;

    private String summary;

    private String coverImage;

    private Long viewCount;

    @TableLogic
    private Integer isDeleted;

    private Integer isPinned;

    private Integer isPublic;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
