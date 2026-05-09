package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserMapper;
import com.note.vo.DashboardVO;
import com.note.vo.NoteVO;
import com.note.vo.TrendVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final UserMapper userMapper;
    private final NoteItemMapper noteItemMapper;

    public DashboardService(UserMapper userMapper, NoteItemMapper noteItemMapper) {
        this.userMapper = userMapper;
        this.noteItemMapper = noteItemMapper;
    }

    @Cacheable(value = "dashboard", key = "'overview'")
    public DashboardVO getOverview() {
        DashboardVO vo = new DashboardVO();

        Long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        Long totalNotes = noteItemMapper.selectCount(new QueryWrapper<com.note.entity.NoteItem>()
                .eq("is_deleted", 0));

        vo.setTotalUsers(totalUsers);
        vo.setTotalNotes(totalNotes);
        Long totalViews = noteItemMapper.sumViewCount();
        vo.setTotalViews(totalViews);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long activeUsersToday = userMapper.selectCount(new QueryWrapper<com.note.entity.User>()
                .ge("last_active_at", todayStart));
        Long newNotesToday = noteItemMapper.selectCount(new QueryWrapper<com.note.entity.NoteItem>()
                .eq("is_deleted", 0)
                .ge("created_at", todayStart));

        vo.setActiveUsersToday(activeUsersToday);
        vo.setNewNotesToday(newNotesToday);

        return vo;
    }

    @Cacheable(value = "dashboard", key = "'userTrend:' + #days")
    public List<TrendVO> getUserTrend(Integer days) {
        if (days == null) days = 7;
        List<TrendVO> trends = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            Long count = userMapper.selectCount(new QueryWrapper<com.note.entity.User>()
                    .ge("created_at", start)
                    .le("created_at", end));

            TrendVO trend = new TrendVO();
            trend.setDate(date.format(formatter));
            trend.setCount(count);
            trends.add(trend);
        }
        return trends;
    }

    @Cacheable(value = "dashboard", key = "'noteTrend:' + #days")
    public List<TrendVO> getNoteTrend(Integer days) {
        if (days == null) days = 7;
        List<TrendVO> trends = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            Long count = noteItemMapper.selectCount(new QueryWrapper<com.note.entity.NoteItem>()
                    .eq("is_deleted", 0)
                    .ge("created_at", start)
                    .le("created_at", end));

            TrendVO trend = new TrendVO();
            trend.setDate(date.format(formatter));
            trend.setCount(count);
            trends.add(trend);
        }
        return trends;
    }

    @Cacheable(value = "dashboard", key = "'topNotes:' + #limit")
    public List<NoteVO> getTopNotes(Integer limit) {
        if (limit == null) limit = 10;
        return noteItemMapper.selectTopNotes(limit);
    }

    public List<TrendVO> getUserActivity() {
        return getUserTrend(7);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    public void clearCache() {}
}
