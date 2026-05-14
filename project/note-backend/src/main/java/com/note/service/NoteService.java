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
        // 检查用户是否设置了主链接
        boolean hasMain = mainLinkService.hasMainLink(userId);
        System.out.println("User " + userId + " has main link: " + hasMain);
        if (!hasMain) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND, "请先设置主链接再发布笔记");
        }

        if (sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }
        if (sensitiveWordService.containsSensitiveWord(dto.getLocationOrUrl())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }

        NoteItem noteItem = new NoteItem();
        noteItem.setUserId(userId);
        noteItem.setTitle(dto.getTitle());
        noteItem.setLocationOrUrl(dto.getLocationOrUrl());
        noteItem.setViewCount(0L);
        noteItem.setIsDeleted(0);
        noteItem.setIsPinned(0);
        noteItem.setCreatedAt(LocalDateTime.now());
        noteItem.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.insert(noteItem);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tagService.syncNoteTags(noteItem.getId(), dto.getTagIds());
        }

        return noteItemMapper.selectNoteWithUserById(noteItem.getId());
    }

    public PageResult<NoteVO> getUserNotes(Long userId, Integer page, Integer size, String keyword, String startDate, String endDate) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        Page<NoteVO> notePage = new Page<>(page, size);
        IPage<NoteVO> result = noteItemMapper.selectNotePageWithUser(notePage, userId, keyword, startDate, endDate);

        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public NoteVO getNoteById(Long noteId) {
        NoteVO vo = noteItemMapper.selectNoteWithUserById(noteId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
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

        if (sensitiveWordService.containsSensitiveWord(dto.getTitle())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }
        if (sensitiveWordService.containsSensitiveWord(dto.getLocationOrUrl())) {
            throw new BusinessException(ErrorCode.NOTE_CONTAINS_SENSITIVE_WORD);
        }

        noteItem.setTitle(dto.getTitle());
        noteItem.setLocationOrUrl(dto.getLocationOrUrl());
        noteItem.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.updateById(noteItem);

        if (dto.getTagIds() != null) {
            tagService.syncNoteTags(noteId, dto.getTagIds());
        }

        return noteItemMapper.selectNoteWithUserById(noteId);
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
