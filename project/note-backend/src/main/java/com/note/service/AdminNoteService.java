package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.dto.NoteDTO;
import com.note.entity.NoteItem;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.OperationLogMapper;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminNoteService {

    private final NoteItemMapper noteItemMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminNoteService(NoteItemMapper noteItemMapper, OperationLogMapper operationLogMapper) {
        this.noteItemMapper = noteItemMapper;
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<NoteVO> listNotes(Integer page, Integer size, String keyword, Integer isDeleted) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<NoteVO> notePage = new Page<>(page, size);
        IPage<NoteVO> result = noteItemMapper.selectNotePageAdmin(notePage, keyword, isDeleted);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public NoteVO getNoteDetail(Long noteId) {
        NoteVO vo = noteItemMapper.selectNoteWithUserById(noteId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        return vo;
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void logicalDeleteNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        noteItemMapper.logicalDelete(noteId, LocalDateTime.now());

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "逻辑删除笔记ID: " + noteId, "NOTE", noteId, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void restoreNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        noteItemMapper.restoreNote(noteId);

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "恢复笔记ID: " + noteId, "NOTE", noteId, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void batchDeleteNotes(List<Long> noteIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long noteId : noteIds) {
            noteItemMapper.logicalDelete(noteId, LocalDateTime.now());
        }

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "批量删除笔记，数量: " + noteIds.size(), "NOTE", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void batchRestoreNotes(List<Long> noteIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long noteId : noteIds) {
            noteItemMapper.restoreNote(noteId);
        }

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "批量恢复笔记，数量: " + noteIds.size(), "NOTE", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public NoteVO createNote(Long userId, NoteDTO dto, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = new NoteItem();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setLocationOrUrl(dto.getLocationOrUrl());
        note.setViewCount(0L);
        note.setIsDeleted(0);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.insert(note);

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "插入笔记，用户ID: " + userId + "，标题: " + dto.getTitle(), "NOTE", note.getId(), ipAddress);

        return noteItemMapper.selectNoteWithUserById(note.getId());
    }

    private void logOperation(Long adminId, String adminUsername, String operationType,
                              String description, String targetType, Long targetId, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setOperationType(operationType);
        log.setDescription(description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
