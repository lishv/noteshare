package com.note.service;

import com.note.entity.NoteTag;
import com.note.entity.Tag;
import com.note.mapper.NoteTagMapper;
import com.note.mapper.TagMapper;
import com.note.vo.PopularTagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {

    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;

    public TagService(TagMapper tagMapper, NoteTagMapper noteTagMapper) {
        this.tagMapper = tagMapper;
        this.noteTagMapper = noteTagMapper;
    }

    public List<Tag> getUserTags(Long userId) {
        return tagMapper.selectByUserId(userId);
    }

    @Transactional
    public Tag createOrGetTag(Long userId, String name) {
        Tag existing = tagMapper.selectByUserId(userId).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(name);
        tagMapper.insert(tag);
        return tag;
    }

    public void syncNoteTags(Long noteId, List<Long> tagIds) {
        noteTagMapper.deleteByNoteId(noteId);
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                NoteTag nt = new NoteTag();
                nt.setNoteId(noteId);
                nt.setTagId(tagId);
                noteTagMapper.insert(nt);
            }
        }
    }

    public List<Long> getTagIdsForNote(Long noteId) {
        return noteTagMapper.selectTagIdsByNoteId(noteId);
    }

    public List<String> getTagNamesForNote(Long noteId) {
        List<Long> tagIds = noteTagMapper.selectTagIdsByNoteId(noteId);
        if (tagIds.isEmpty()) return new ArrayList<>();
        return tagIds.stream().map(id -> {
            Tag t = tagMapper.selectById(id);
            return t != null ? t.getName() : null;
        }).filter(n -> n != null).toList();
    }

    public List<Long> getNoteIdsByTagId(Long tagId) {
        return noteTagMapper.selectNoteIdsByTagId(tagId);
    }

    @Transactional
    public void deleteTag(Long tagId, Long userId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null || !tag.getUserId().equals(userId)) return;
        noteTagMapper.deleteByTagId(tagId);
        tagMapper.deleteById(tagId);
    }

    public List<PopularTagVO> getPopularTags(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return tagMapper.selectPopularTags(limit);
    }
}
