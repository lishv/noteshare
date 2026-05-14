package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_note_favorite")
public class NoteFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("note_id")
    private Long noteId;

    private LocalDateTime createdAt;
}
