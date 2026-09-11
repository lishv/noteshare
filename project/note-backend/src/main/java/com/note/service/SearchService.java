package com.note.service;

import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.NoteVO;
import com.note.vo.SearchVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final UserMapper userMapper;
    private final NoteItemMapper noteItemMapper;

    public SearchService(UserMapper userMapper, NoteItemMapper noteItemMapper) {
        this.userMapper = userMapper;
        this.noteItemMapper = noteItemMapper;
    }

    public SearchVO search(String keyword, Long currentUserId, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        SearchVO result = new SearchVO();
        result.setPage(page);
        result.setSize(size);

        Page<ActiveUserVO> userPage = new Page<>(1, 10);
        IPage<ActiveUserVO> userResult = userMapper.searchUsers(userPage, keyword, null);
        result.setUsers(userResult.getRecords());

        Page<NoteVO> notePage = new Page<>(1, 10);
        IPage<NoteVO> noteResult = noteItemMapper.searchNotesWithUser(notePage, keyword, currentUserId);
        result.setNotes(noteResult.getRecords());

        result.setTotal(userResult.getTotal() + noteResult.getTotal());
        return result;
    }
}
