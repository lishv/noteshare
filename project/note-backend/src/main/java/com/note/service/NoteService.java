package com.note.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.dto.NoteDTO;
import com.note.dto.NoteDraftDTO;
import com.note.entity.NoteItem;
import com.note.entity.NoteLike;
import com.note.entity.Tag;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NoteFavoriteMapper;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.NoteLikeMapper;
import com.note.mapper.UserMapper;
import com.note.vo.NoteDraftVO;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class NoteService {

    private final NoteItemMapper noteItemMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final SensitiveWordService sensitiveWordService;
    private final MainLinkService mainLinkService;
    private final TagService tagService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final String DRAFT_KEY_PREFIX = "note:draft:";
    private static final long DRAFT_TTL_DAYS = 14;

    public NoteService(NoteItemMapper noteItemMapper,
                       NoteLikeMapper noteLikeMapper,
                       NoteFavoriteMapper noteFavoriteMapper,
                       SensitiveWordService sensitiveWordService,
                       MainLinkService mainLinkService,
                       TagService tagService,
                       NotificationService notificationService,
                       UserMapper userMapper,
                       RedisTemplate<String, Object> redisTemplate,
                       ObjectMapper objectMapper,
                       JdbcTemplate jdbcTemplate) {
        this.noteItemMapper = noteItemMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.noteFavoriteMapper = noteFavoriteMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.mainLinkService = mainLinkService;
        this.tagService = tagService;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
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

        List<Long> finalTagIds = resolveTagIds(userId, dto.getTagIds(), dto.getTagNames());
        if (!finalTagIds.isEmpty()) {
            tagService.syncNoteTags(noteItem.getId(), finalTagIds);
        }

        // 新建笔记成功后，自动清除该用户的“新建草稿”缓存
        deleteDraft(userId, null);

        NoteVO result = noteItemMapper.selectNoteWithUserById(noteItem.getId());
        if (result != null) {
            result.setTags(tagService.getTagNamesForNote(result.getId()));
        }
        return result;
    }

    public PageResult<NoteVO> getUserNotes(Long userId, Long currentUserId, Integer page, Integer size, String keyword, Integer noteType, String startDate, String endDate) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        Page<NoteVO> notePage = new Page<>(page, size);
        IPage<NoteVO> result = noteItemMapper.selectNotePageWithUser(notePage, userId, currentUserId, keyword, noteType, startDate, endDate);
        populateTags(result.getRecords());

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

        // 填充标签列表
        vo.setTags(tagService.getTagNamesForNote(noteId));

        // 填充互动数据与用户状态
        try {
            long likes = noteLikeMapper.countByNoteId(noteId);
            vo.setLikeCount(likes);
            if (currentUserId != null) {
                vo.setIsLiked(noteLikeMapper.existsByUserAndNote(currentUserId, noteId) > 0);
                vo.setIsFavorited(noteFavoriteMapper.existsByUserAndNote(currentUserId, noteId) > 0);
            } else {
                vo.setIsLiked(false);
                vo.setIsFavorited(false);
            }
            int favTotal = noteFavoriteMapper.countByNoteId(noteId);
            vo.setFavoriteCount((long) favTotal);
            Integer commTotal = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM t_comment WHERE note_id = ? AND is_deleted = 0", Integer.class, noteId);
            vo.setCommentCount(commTotal != null ? commTotal.longValue() : 0L);
        } catch (Exception ignored) {}

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

        if (dto.getTagIds() != null || dto.getTagNames() != null) {
            List<Long> finalTagIds = resolveTagIds(userId, dto.getTagIds(), dto.getTagNames());
            tagService.syncNoteTags(noteId, finalTagIds);
        }

        // 更新笔记成功后，自动清除该笔记的草稿缓存
        deleteDraft(userId, noteId);

        NoteVO updatedVo = noteItemMapper.selectNoteWithUserById(noteId);
        if (updatedVo != null) {
            updatedVo.setTags(tagService.getTagNamesForNote(noteId));
        }
        return updatedVo;
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

    /**
     * 保存/更新草稿至 Redis
     */
    public NoteDraftVO saveDraft(Long userId, NoteDraftDTO dto) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        NoteDraftVO vo = new NoteDraftVO();
        vo.setNoteId(dto.getNoteId());
        vo.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        vo.setContent(dto.getContent() != null ? dto.getContent() : "");
        vo.setCoverImage(dto.getCoverImage());
        vo.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 1);
        vo.setTagIds(dto.getTagIds());
        vo.setSavedAt(LocalDateTime.now());

        String key = getDraftKey(userId, dto.getNoteId());
        try {
            redisTemplate.opsForValue().set(key, vo, DRAFT_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            // 如果 Redis 异常，记录日志并抛出或降级
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存草稿失败: " + e.getMessage());
        }
        return vo;
    }

    /**
     * 从 Redis 获取草稿
     */
    public NoteDraftVO getDraft(Long userId, Long noteId) {
        if (userId == null) {
            return null;
        }
        String key = getDraftKey(userId, noteId);
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj == null) {
                return null;
            }
            if (obj instanceof NoteDraftVO) {
                return (NoteDraftVO) obj;
            }
            // Jackson 序列化回退处理
            return objectMapper.convertValue(obj, NoteDraftVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Redis 删除草稿
     */
    public void deleteDraft(Long userId, Long noteId) {
        if (userId == null) {
            return;
        }
        String key = getDraftKey(userId, noteId);
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    private String getDraftKey(Long userId, Long noteId) {
        String notePart = (noteId != null && noteId > 0) ? String.valueOf(noteId) : "new";
        return DRAFT_KEY_PREFIX + userId + ":" + notePart;
    }

    // ==================== 笔记回收站 ====================

    /**
     * 获取用户回收站笔记列表
     */
    public PageResult<NoteVO> getRecycleBinNotes(Long userId, Integer page, Integer size) {
        int p = (page != null && page > 0) ? page : 1;
        int s = (size != null && size > 0) ? size : 10;
        Page<NoteVO> pageParam = new Page<>(p, s);
        IPage<NoteVO> result = noteItemMapper.selectRecycleBinNotes(pageParam, userId);
        return PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords());
    }

    /**
     * 从回收站恢复笔记
     */
    @Transactional
    public void restoreNote(Long noteId, Long userId) {
        int rows = noteItemMapper.restoreUserNote(noteId, userId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND, "未找到该回收站笔记或无权恢复");
        }
    }

    /**
     * 彻底物理删除笔记
     */
    @Transactional
    public void hardDeleteNote(Long noteId, Long userId) {
        NoteItem note = noteItemMapper.selectById(noteId);
        if (note == null || note.getIsDeleted() != 1 || !userId.equals(note.getUserId())) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND, "未找到该回收站笔记或无权彻底删除");
        }
        // 先清理关联数据，再删除父笔记，避免外键约束或孤儿记录。
        deleteDraft(userId, noteId);
        jdbcTemplate.update("DELETE FROM t_note_favorite WHERE note_id = ?", noteId);
        jdbcTemplate.update("DELETE FROM t_note_like WHERE note_id = ?", noteId);
        jdbcTemplate.update("DELETE FROM t_note_tag WHERE note_id = ?", noteId);
        jdbcTemplate.update("DELETE FROM t_comment WHERE note_id = ?", noteId);
        int rows = noteItemMapper.hardDeleteUserNote(noteId, userId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND, "未找到该回收站笔记或无权彻底删除");
        }
    }

    /**
     * 一键清空回收站
     */
    @Transactional
    public void clearRecycleBin(Long userId) {
        List<Long> noteIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_note_item WHERE user_id = ? AND is_deleted = 1", Long.class, userId);
        for (Long noteId : noteIds) {
            deleteDraft(userId, noteId);
            jdbcTemplate.update("DELETE FROM t_note_favorite WHERE note_id = ?", noteId);
            jdbcTemplate.update("DELETE FROM t_note_like WHERE note_id = ?", noteId);
            jdbcTemplate.update("DELETE FROM t_note_tag WHERE note_id = ?", noteId);
            jdbcTemplate.update("DELETE FROM t_comment WHERE note_id = ?", noteId);
        }
        if (!noteIds.isEmpty()) {
            noteItemMapper.clearUserRecycleBin(userId);
        }
    }

    // ==================== 关注动态流 ====================

    /**
     * 分页获取关注作者的最新公开笔记流
     */
    public PageResult<NoteVO> getFollowingFeed(Long currentUserId, Integer page, Integer size) {
        if (currentUserId == null) {
            return PageResult.of(0L, 1, size != null ? size : 10, java.util.Collections.emptyList());
        }
        int p = (page != null && page > 0) ? page : 1;
        int s = (size != null && size > 0) ? size : 10;
        Page<NoteVO> pageParam = new Page<>(p, s);
        IPage<NoteVO> result = noteItemMapper.selectFollowingFeed(pageParam, currentUserId);
        populateTags(result.getRecords());
        return PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords());
    }

    // ==================== 热度榜与公共推荐 ====================

    /**
     * 获取全站热度榜（热门笔记）
     */
    public java.util.List<NoteVO> getTrendingNotes(Integer limit) {
        int l = (limit != null && limit > 0 && limit <= 50) ? limit : 10;
        List<NoteVO> list = noteItemMapper.selectTrendingNotes(l);
        populateTags(list);
        return list;
    }

    /**
     * 获取公共主页推荐笔记列表（热度算法推荐，默认一页6条）
     */
    public PageResult<NoteVO> getRecommendedNotes(Integer page, Integer size, Long currentUserId, String tag) {
        int p = (page != null && page > 0) ? page : 1;
        int s = (size != null && size > 0) ? size : 6;
        Page<NoteVO> pageParam = new Page<>(p, s);
        IPage<NoteVO> result = noteItemMapper.selectRecommendedNotes(pageParam, currentUserId, tag);
        populateTags(result.getRecords());
        return PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords());
    }

    private void populateTags(List<NoteVO> records) {
        if (records == null || records.isEmpty()) return;
        for (NoteVO vo : records) {
            if (vo != null && vo.getId() != null) {
                vo.setTags(tagService.getTagNamesForNote(vo.getId()));
            }
        }
    }

    private List<Long> resolveTagIds(Long userId, List<Long> tagIds, List<String> tagNames) {
        List<Long> result = new ArrayList<>();
        if (tagIds != null) {
            result.addAll(tagIds);
        }
        if (tagNames != null) {
            for (String name : tagNames) {
                if (name != null && !name.trim().isEmpty()) {
                    Tag tag = tagService.createOrGetTag(userId, name.trim());
                    if (!result.contains(tag.getId())) {
                        result.add(tag.getId());
                    }
                }
            }
        }
        return result;
    }

    // ==================== 点赞机制 ====================

    /**
     * 点赞笔记
     */
    @Transactional
    public void likeNote(Long noteId, Long userId) {
        NoteItem note = noteItemMapper.selectById(noteId);
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        if (noteLikeMapper.existsByUserAndNote(userId, noteId) > 0) {
            return; // 幂等成功
        }

        NoteLike like = new NoteLike();
        like.setUserId(userId);
        like.setNoteId(noteId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            noteLikeMapper.insert(like);
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // 并发重复点赞视为幂等成功。
            return;
        }

        // t_note_like 是权威数据；计数列仅作为兼容旧页面的缓存字段维护。
        jdbcTemplate.update("UPDATE t_note_item SET like_count = like_count + 1 WHERE id = ? AND is_deleted = 0", noteId);

        // 发送点赞站内通知给笔记作者
        User liker = userMapper.selectById(userId);
        String likerName = liker != null ? liker.getUsername() : "用户";
        notificationService.sendNotification(
                note.getUserId(),
                userId,
                "LIKE",
                likerName + " 赞了你的笔记《" + note.getTitle() + "》",
                note.getSummary() != null ? note.getSummary() : "",
                "NOTE",
                noteId
        );
    }

    /**
     * 取消点赞笔记
     */
    @Transactional
    public void unlikeNote(Long noteId, Long userId) {
        int rows = noteLikeMapper.deleteByUserAndNote(userId, noteId);
        if (rows > 0) {
            jdbcTemplate.update("UPDATE t_note_item SET like_count = GREATEST(0, like_count - 1) WHERE id = ? AND is_deleted = 0", noteId);
        }
    }

    /**
     * 检查当前用户是否已点赞
     */
    public boolean checkLiked(Long noteId, Long userId) {
        if (userId == null || noteId == null) {
            return false;
        }
        return noteLikeMapper.existsByUserAndNote(userId, noteId) > 0;
    }
}
