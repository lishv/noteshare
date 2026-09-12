package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserFollowMapper;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.NoteVO;
import com.note.vo.SearchVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final UserMapper userMapper;
    private final NoteItemMapper noteItemMapper;
    private final UserFollowMapper userFollowMapper;
    private final TagService tagService;

    public SearchService(UserMapper userMapper, NoteItemMapper noteItemMapper,
                         UserFollowMapper userFollowMapper, TagService tagService) {
        this.userMapper = userMapper;
        this.noteItemMapper = noteItemMapper;
        this.userFollowMapper = userFollowMapper;
        this.tagService = tagService;
    }

    public SearchVO search(String keyword, Long currentUserId, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 50 : size;

        SearchVO result = new SearchVO();
        result.setPage(currentPage);
        result.setSize(pageSize);

        String searchKeyword = keyword == null ? "" : keyword.trim();

        // 1. 仅检索符合条件的公开、未删除笔记
        Page<NoteVO> notePage = new Page<>(currentPage, pageSize);
        IPage<NoteVO> noteResult = noteItemMapper.searchNotesWithUser(notePage, searchKeyword, currentUserId);
        List<NoteVO> noteRecords = noteResult.getRecords();
        for (NoteVO vo : noteRecords) {
            if (vo != null && vo.getId() != null) {
                vo.setTags(tagService.getTagNamesForNote(vo.getId()));
            }
        }
        result.setNotes(noteRecords);

        // 2. 汇总搜索结果中涉及的创作者（保证 noteCount / followerCount 真实完整）
        Map<Long, ActiveUserVO> userMap = new LinkedHashMap<>();

        // 优先将包含匹配笔记的创作者纳入（对应左侧匹配创作者展示）
        for (NoteVO note : noteRecords) {
            if (note != null && note.getUserId() != null && !userMap.containsKey(note.getUserId())) {
                ActiveUserVO author = userMapper.selectActiveUserById(note.getUserId());
                if (author != null) {
                    if (currentUserId != null && !currentUserId.equals(author.getId())) {
                        author.setIsFollowing(userFollowMapper.checkIsFollowing(currentUserId, author.getId()) > 0);
                    } else {
                        author.setIsFollowing(false);
                    }
                    userMap.put(author.getId(), author);
                }
            }
        }

        // 辅以直接按作者姓名/邮箱模糊匹配出的用户
        Page<ActiveUserVO> userPage = new Page<>(1, 20);
        IPage<ActiveUserVO> userResult = userMapper.searchUsers(userPage, searchKeyword, null);
        for (ActiveUserVO u : userResult.getRecords()) {
            if (u != null && u.getId() != null && !userMap.containsKey(u.getId())) {
                if (currentUserId != null && !currentUserId.equals(u.getId())) {
                    u.setIsFollowing(userFollowMapper.checkIsFollowing(currentUserId, u.getId()) > 0);
                } else {
                    u.setIsFollowing(false);
                }
                userMap.put(u.getId(), u);
            }
        }

        List<ActiveUserVO> allAuthors = new ArrayList<>(userMap.values());
        result.setUsers(allAuthors);
        result.setTotal(noteResult.getTotal());
        return result;
    }
}
