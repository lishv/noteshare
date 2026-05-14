package com.note.service;

import com.note.entity.NoteFavorite;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NoteFavoriteMapper;
import com.note.mapper.NoteItemMapper;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteService {

    private final NoteFavoriteMapper favoriteMapper;
    private final NoteItemMapper noteItemMapper;

    public FavoriteService(NoteFavoriteMapper favoriteMapper, NoteItemMapper noteItemMapper) {
        this.favoriteMapper = favoriteMapper;
        this.noteItemMapper = noteItemMapper;
    }

    public void addFavorite(Long userId, Long noteId) {
        if (favoriteMapper.existsByUserAndNote(userId, noteId) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已收藏该笔记");
        }
        NoteFavorite fav = new NoteFavorite();
        fav.setUserId(userId);
        fav.setNoteId(noteId);
        fav.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(fav);
    }

    public void removeFavorite(Long userId, Long noteId) {
        if (favoriteMapper.deleteByUserAndNote(userId, noteId) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未收藏该笔记");
        }
    }

    public boolean isFavorited(Long userId, Long noteId) {
        return userId != null && favoriteMapper.existsByUserAndNote(userId, noteId) > 0;
    }

    public PageResult<NoteVO> listMyFavorites(Long userId, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        List<Long> noteIds = favoriteMapper.selectFavoriteNoteIds(userId);
        if (noteIds.isEmpty()) {
            return PageResult.of(0L, page, size, List.of());
        }

        int total = noteIds.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        if (fromIndex >= total) {
            return PageResult.of((long) total, page, size, List.of());
        }

        List<Long> pageIds = noteIds.subList(fromIndex, toIndex);
        List<NoteVO> notes = pageIds.stream()
                .map(noteItemMapper::selectNoteWithUserById)
                .filter(n -> n != null)
                .toList();

        return PageResult.of((long) total, page, size, notes);
    }
}
