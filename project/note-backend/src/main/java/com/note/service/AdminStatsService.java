package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.note.entity.NoteItem;
import com.note.entity.User;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserMapper;
import com.note.vo.DistributionVO;
import com.note.vo.NoteVO;
import com.note.vo.TrendVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStatsService {

    private final UserMapper userMapper;
    private final NoteItemMapper noteItemMapper;

    public AdminStatsService(UserMapper userMapper, NoteItemMapper noteItemMapper) {
        this.userMapper = userMapper;
        this.noteItemMapper = noteItemMapper;
    }

    public List<TrendVO> getRegistrationTrend(Integer days) {
        if (days == null) days = 30;
        return buildTrend(days, "user");
    }

    public List<TrendVO> getActivityStats(String period) {
        if (period == null) period = "daily";
        int days = "weekly".equals(period) ? 7 : "monthly".equals(period) ? 30 : 7;
        return buildTrend(days, "activity");
    }

    public List<DistributionVO> getUserNoteDistribution() {
        List<DistributionVO> distribution = new ArrayList<>();

        String[] ranges = {"0", "1-5", "6-10", "11-20", "20+"};
        long[] counts = new long[5];

        List<User> users = userMapper.selectList(null);
        for (User user : users) {
            Long noteCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                    .eq("user_id", user.getId())
                    .eq("is_deleted", 0));
            int c = noteCount.intValue();
            if (c == 0) counts[0]++;
            else if (c <= 5) counts[1]++;
            else if (c <= 10) counts[2]++;
            else if (c <= 20) counts[3]++;
            else counts[4]++;
        }

        for (int i = 0; i < ranges.length; i++) {
            DistributionVO vo = new DistributionVO();
            vo.setRange(ranges[i]);
            vo.setCount(counts[i]);
            distribution.add(vo);
        }
        return distribution;
    }

    public List<TrendVO> getPublishTrend(Integer days) {
        if (days == null) days = 30;
        return buildTrend(days, "note");
    }

    public List<NoteVO> getViewRanking(Integer limit) {
        if (limit == null) limit = 20;
        return noteItemMapper.selectTopNotes(limit);
    }

    public List<DistributionVO> getLinkTypeStats() {
        List<DistributionVO> stats = new ArrayList<>();

        Long urlCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .likeRight("location_or_url", "http"));
        Long relativeCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .notLikeRight("location_or_url", "http"));

        DistributionVO urlVo = new DistributionVO();
        urlVo.setRange("URL链接");
        urlVo.setCount(urlCount);
        stats.add(urlVo);

        DistributionVO relVo = new DistributionVO();
        relVo.setRange("相对位置");
        relVo.setCount(relativeCount);
        stats.add(relVo);

        return stats;
    }

    public List<TrendVO> getPlatformTrends(Integer days) {
        if (days == null) days = 30;
        return buildTrend(days, "platform");
    }

    private List<TrendVO> buildTrend(int days, String type) {
        List<TrendVO> trends = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            Long count;

            switch (type) {
                case "user":
                    count = userMapper.selectCount(new QueryWrapper<User>().ge("created_at", start).le("created_at", end));
                    break;
                case "note":
                case "platform":
                    count = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).ge("created_at", start).le("created_at", end));
                    break;
                case "activity":
                    count = userMapper.selectCount(new QueryWrapper<User>().ge("last_active_at", start).le("last_active_at", end));
                    break;
                default:
                    count = 0L;
            }

            TrendVO trend = new TrendVO();
            trend.setDate(date.format(formatter));
            trend.setCount(count);
            trends.add(trend);
        }
        return trends;
    }
}
