package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.dto.NoteDTO;
import com.note.entity.NoteItem;
import com.note.entity.OperationLog;
import com.note.entity.Tag;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminNoteService {

    private final NoteItemMapper noteItemMapper;
    private final OperationLogMapper operationLogMapper;
    private final NotificationService notificationService;
    private final TagService tagService;

    public AdminNoteService(NoteItemMapper noteItemMapper,
                            OperationLogMapper operationLogMapper,
                            NotificationService notificationService,
                            TagService tagService) {
        this.noteItemMapper = noteItemMapper;
        this.operationLogMapper = operationLogMapper;
        this.notificationService = notificationService;
        this.tagService = tagService;
    }

    public PageResult<NoteVO> listNotes(Integer page, Integer size, String keyword, Integer noteType, Integer isPublic, Integer isDeleted) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<NoteVO> notePage = new Page<>(page, size);
        IPage<NoteVO> result = noteItemMapper.selectNotePageAdmin(notePage, keyword, noteType, isPublic, isDeleted);
        for (NoteVO vo : result.getRecords()) {
            if (vo != null && vo.getId() != null) {
                vo.setTags(tagService.getTagNamesForNote(vo.getId()));
            }
        }
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public NoteVO getNoteDetail(Long noteId) {
        NoteVO vo = noteItemMapper.selectNoteWithUserById(noteId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        vo.setTags(tagService.getTagNamesForNote(noteId));
        return vo;
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void logicalDeleteNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = noteItemMapper.selectById(noteId);
        noteItemMapper.logicalDelete(noteId, LocalDateTime.now());

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "逻辑删除笔记ID: " + noteId, "NOTE", noteId, ipAddress);

        if (note != null) {
            try {
                notificationService.sendNotification(
                        note.getUserId(),
                        null,
                        "SYSTEM",
                        "系统通知：您的笔记《" + note.getTitle() + "》已被管理员下架",
                        "由于内容调整或违规核查，管理员已对该笔记执行下架处理。",
                        "NOTE",
                        noteId
                );
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void restoreNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = noteItemMapper.selectById(noteId);
        noteItemMapper.restoreNote(noteId);

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "恢复笔记ID: " + noteId, "NOTE", noteId, ipAddress);

        if (note != null) {
            try {
                notificationService.sendNotification(
                        note.getUserId(),
                        null,
                        "SYSTEM",
                        "系统通知：您的笔记《" + note.getTitle() + "》已被管理员恢复上架",
                        "管理员已审核并恢复了您的笔记展示状态。",
                        "NOTE",
                        noteId
                );
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void batchDeleteNotes(List<Long> noteIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long noteId : noteIds) {
            NoteItem note = noteItemMapper.selectById(noteId);
            noteItemMapper.logicalDelete(noteId, LocalDateTime.now());
            if (note != null) {
                try {
                    notificationService.sendNotification(
                            note.getUserId(),
                            null,
                            "SYSTEM",
                            "系统通知：您的笔记《" + note.getTitle() + "》已被管理员下架",
                            "由于内容调整或违规核查，管理员已对该笔记执行下架处理。",
                            "NOTE",
                            noteId
                    );
                } catch (Exception ignored) {}
            }
        }

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "批量删除笔记，数量: " + noteIds.size(), "NOTE", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void batchRestoreNotes(List<Long> noteIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long noteId : noteIds) {
            NoteItem note = noteItemMapper.selectById(noteId);
            noteItemMapper.restoreNote(noteId);
            if (note != null) {
                try {
                    notificationService.sendNotification(
                            note.getUserId(),
                            null,
                            "SYSTEM",
                            "系统通知：您的笔记《" + note.getTitle() + "》已被管理员恢复上架",
                            "管理员已审核并恢复了您的笔记展示状态。",
                            "NOTE",
                            noteId
                    );
                } catch (Exception ignored) {}
            }
        }

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "批量恢复笔记，数量: " + noteIds.size(), "NOTE", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public NoteVO createNote(Long userId, NoteDTO dto, Long adminId, String adminUsername, String ipAddress) {
        int type = (dto.getNoteType() != null && dto.getNoteType() == 2) ? 2 : 1;

        NoteItem note = new NoteItem();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setNoteType(type);
        note.setLocationOrUrl(dto.getLocationOrUrl() != null ? dto.getLocationOrUrl() : "");
        note.setContent(dto.getContent());
        note.setCoverImage(dto.getCoverImage());

        if (dto.getSummary() != null && !dto.getSummary().trim().isEmpty()) {
            note.setSummary(dto.getSummary().trim());
        } else if (type == 2 && dto.getContent() != null) {
            String plainText = dto.getContent()
                    .replaceAll("(?s)```.*?```", " ")
                    .replaceAll("`.*?`", " ")
                    .replaceAll("#+\\s*", "")
                    .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                    .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1")
                    .replaceAll("[*_~`>\\-\\+]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            note.setSummary(plainText.length() > 150 ? plainText.substring(0, 150) + "..." : plainText);
        }

        note.setViewCount(0L);
        note.setIsDeleted(0);
        note.setIsPinned(0);
        note.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 1);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        noteItemMapper.insert(note);

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "插入笔记，用户ID: " + userId + "，标题: " + dto.getTitle(), "NOTE", note.getId(), ipAddress);

        try {
            notificationService.sendNotification(
                    userId,
                    null,
                    "SYSTEM",
                    "系统通知：管理员为您添加了新笔记《" + dto.getTitle() + "》",
                    "管理员已在您的知识库中录入了一篇新笔记，欢迎查看。",
                    "NOTE",
                    note.getId()
            );
        } catch (Exception ignored) {}

        return noteItemMapper.selectNoteWithUserById(note.getId());
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void pinNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = noteItemMapper.selectById(noteId);
        noteItemMapper.updatePinned(noteId, 1);
        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "置顶笔记ID: " + noteId, "NOTE", noteId, ipAddress);

        if (note != null) {
            try {
                notificationService.sendNotification(
                        note.getUserId(),
                        null,
                        "SYSTEM",
                        "系统通知：您的笔记《" + note.getTitle() + "》已被设为精选置顶",
                        "恭喜！您的笔记内容优质，已被管理员设为首页精选置顶展示。",
                        "NOTE",
                        noteId
                );
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void unpinNote(Long noteId, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = noteItemMapper.selectById(noteId);
        noteItemMapper.updatePinned(noteId, 0);
        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "取消置顶笔记ID: " + noteId, "NOTE", noteId, ipAddress);

        if (note != null) {
            try {
                notificationService.sendNotification(
                        note.getUserId(),
                        null,
                        "SYSTEM",
                        "系统通知：您的笔记《" + note.getTitle() + "》已取消置顶",
                        "您的笔记置顶推荐期已结束，现已恢复常规展示。",
                        "NOTE",
                        noteId
                );
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void updateNoteTags(Long noteId, List<String> tagNames, Long adminId, String adminUsername, String ipAddress) {
        NoteItem note = noteItemMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }
        List<Long> tagIds = new ArrayList<>();
        if (tagNames != null) {
            for (String name : tagNames) {
                if (name != null && !name.trim().isEmpty()) {
                    Tag tag = tagService.createOrGetTag(note.getUserId(), name.trim());
                    if (!tagIds.contains(tag.getId())) {
                        tagIds.add(tag.getId());
                    }
                }
            }
        }
        tagService.syncNoteTags(noteId, tagIds);

        logOperation(adminId, adminUsername, "NOTE_MANAGE",
                "修改笔记标签，笔记ID: " + noteId + "，新标签: " + (tagNames != null ? tagNames : "[]"),
                "NOTE", noteId, ipAddress);

        try {
            String tagSummary = (tagNames == null || tagNames.isEmpty()) ? "无" : String.join(", ", tagNames);
            notificationService.sendNotification(
                    note.getUserId(),
                    null,
                    "SYSTEM",
                    "系统通知：管理员更新了您笔记《" + note.getTitle() + "》的标签",
                    "管理员为您维护了笔记分类标签：[" + tagSummary + "]，以便更多读者发现。",
                    "NOTE",
                    noteId
            );
        } catch (Exception ignored) {}
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
