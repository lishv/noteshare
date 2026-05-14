package com.note.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_note_tag")
public class NoteTag {
    private Long noteId;
    private Long tagId;
}
