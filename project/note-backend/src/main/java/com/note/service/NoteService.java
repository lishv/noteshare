package com.note.service;

import com.note.dto.NoteDTO;
import com.note.entity.NoteItem;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NoteItemMapper;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NoteService {

    private final NoteItemMapper noteItemMapper;
    private final SensitiveWordService sensitiveWordService;
    private final MainLinkService mainLinkService;
    private final TagService tagService;

    public NoteService(NoteItemMapper noteItemMapper, SensitiveWordService sensitiveWordService, MainLinkService mainLinkService, TagService tagService) {
        this.noteItemMapper = noteItemMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.mainLinkService = mainLinkService;
        this.tagService = tagService;
    }

    @Transactional
    public NoteVO createNote(Long userId, NoteDTO dto) {
        int type = (dto.getNoteType() != null && dto.getNoteType() == 2) ? 2 : 1;

        if (type == 1) {
            // 链接笔记：检查用户是否设置了主链接
            boolean hasMain = mainLinkService.hasMainLink(userId);
            if (!hasMain) {
                throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND, "请先设置总链接再发布链接笔记");
            }
            if (dto.getLocationOrUrl() == null || dto.getLocationOrUrl().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "链接/位置不能为空");
            }
            if (sensitiveWordService.containsSensitiveWord(dto.getLocationOrUrl())) {
                throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
            }
        } else {
            // 创作笔记：无需主链接，校验正文内容
            if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "笔记内容不能为空");
            }
            if (sensitiveWordService.containsSensitiveWord(dto.getContent())) {
                throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
            }
        }

        if (sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }

        NoteItem noteItem = new NoteItem();
        noteItem.setUserId(userId);
        noteItem.setTitle(dto.getTitle());
        noteItem.setNoteType(type);
        noteItem.setLocationOrUrl(dto.getLocationOrUrl() != null ? dto.getLocationOrUrl() : "");
        noteItem.setContent(dto.getContent());
        noteItem.setCoverImage(dto.getCoverImage());

        // 生成摘要
        if (dto.getSummary() != null && !dto.getSummary().trim().isEmpty()) {
            noteItem.setSummary(dto.getSummary().trim());
        } else if (type == 2 && dto.getContent() != null) {
            noteItem.setSummary(extractSummary(dto.getContent()));
        }

        noteItem.setViewCount(0L);
        noteItem.setIsDeleted(0);
        noteItem.setIsPinned(0);
        noteItem.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 1);
        noteItem.setCreatedAt(LocalDateTime.now());
        noteItem.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.insert(noteItem);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tagService.syncNoteTags(noteItem.getId(), dto.getTagIds());
        }

        return noteItemMapper.selectNoteWithUserById(noteItem.getId());
    }

    public PageResult<NoteVO> getUserNotes(Long userId, Long currentUserId, Integer page, Integer size, String keyword, Integer noteType, String startDate, String endDate) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        Page<NoteVO> notePage = new Page<>(page, size);
        IPage<NoteVO> result = noteItemMapper.selectNotePageWithUser(notePage, userId, currentUserId, keyword, noteType, startDate, endDate);

        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public NoteVO getNoteById(Long noteId, Long currentUserId) {
        NoteVO vo = noteItemMapper.selectNoteWithUserById(noteId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        // 私密笔记权限校验：若为私密笔记，仅允许作者本人访问
        if (vo.getIsPublic() != null && vo.getIsPublic() == 0) {
            if (currentUserId == null || !currentUserId.equals(vo.getUserId())) {
                throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED, "该笔记为私密笔记，仅作者本人可见");
            }
        }

        noteItemMapper.incrementViewCount(noteId);
        vo.setViewCount(vo.getViewCount() + 1);

        return vo;
    }

    @Transactional
    public NoteVO updateNote(Long noteId, Long userId, NoteDTO dto) {
        NoteItem noteItem = noteItemMapper.selectById(noteId);
        if (noteItem == null || noteItem.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (!noteItem.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
        }

        int type = dto.getNoteType() != null ? dto.getNoteType() : (noteItem.getNoteType() != null ? noteItem.getNoteType() : 1);

        if (type == 1) {
            if (dto.getLocationOrUrl() != null && sensitiveWordService.containsSensitiveWord(dto.getLocationOrUrl())) {
                throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
            }
        } else {
            if (dto.getContent() != null && sensitiveWordService.containsSensitiveWord(dto.getContent())) {
                throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
            }
        }

        if (dto.getTitle() != null && sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }

        noteItem.setTitle(dto.getTitle());
        noteItem.setNoteType(type);
        if (dto.getLocationOrUrl() != null) {
            noteItem.setLocationOrUrl(dto.getLocationOrUrl());
        } else if (noteItem.getLocationOrUrl() == null) {
            noteItem.setLocationOrUrl("");
        }
        noteItem.setContent(dto.getContent());
        if (dto.getCoverImage() != null) {
            noteItem.setCoverImage(dto.getCoverImage());
        }
        if (dto.getIsPublic() != null) {
            noteItem.setIsPublic(dto.getIsPublic());
        }

        if (dto.getSummary() != null && !dto.getSummary().trim().isEmpty()) {
            noteItem.setSummary(dto.getSummary().trim());
        } else if (type == 2 && dto.getContent() != null) {
            noteItem.setSummary(extractSummary(dto.getContent()));
        }

        noteItem.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.updateById(noteItem);

        if (dto.getTagIds() != null) {
            tagService.syncNoteTags(noteId, dto.getTagIds());
        }

        return noteItemMapper.selectNoteWithUserById(noteId);
    }

    private String extractSummary(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        // 简单去除 Markdown 常用标记
        String plainText = markdown
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("`.*?`", " ")
                .replaceAll("#+\\s*", "")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1")
                .replaceAll("[*_~`>\\-\\+]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (plainText.length() > 150) {
            return plainText.substring(0, 150) + "...";
        }
        return plainText;
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        NoteItem noteItem = noteItemMapper.selectById(noteId);
        if (noteItem == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (!noteItem.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
        }

        noteItemMapper.logicalDelete(noteId, LocalDateTime.now());
    }

    public void incrementViewCount(Long noteId) {
        noteItemMapper.incrementViewCount(noteId);
    }

    @Transactional
    public void pinNote(Long noteId, Long userId) {
        NoteItem noteItem = noteItemMapper.selectById(noteId);
        if (noteItem == null || noteItem.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        if (!noteItem.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
        }
        noteItem.setIsPinned(1);
        noteItem.setUpdatedAt(LocalDateTime.now());
        noteItemMapper.updateById(noteItem);
    }

    @Transactional
    public void unpinNote(Long noteId, Long userId) {
        NoteItem noteItem = noteItemMapper.selectById(noteId);
        if (noteItem == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        if (!noteItem.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
        }
        noteItem.setIsPinned(0);
        noteItem.setUpdatedAt(LocalDateTime.now());
        noteItemMapper.updateById(noteItem);
    }
}
