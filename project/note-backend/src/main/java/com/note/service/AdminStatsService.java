package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.note.entity.Comment;
import com.note.entity.MainLink;
import com.note.entity.NoteFavorite;
import com.note.entity.NoteItem;
import com.note.entity.User;
import com.note.entity.UserFollow;
import com.note.mapper.CommentMapper;
import com.note.mapper.MainLinkMapper;
import com.note.mapper.NoteFavoriteMapper;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserFollowMapper;
import com.note.mapper.UserMapper;
import com.note.vo.DistributionVO;
import com.note.vo.NoteVO;
import com.note.vo.PlatformSummaryVO;
import com.note.vo.TrendVO;
import org.springframework.cache.annotation.Cacheable;
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
    private final CommentMapper commentMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final UserFollowMapper userFollowMapper;
    private final MainLinkMapper mainLinkMapper;

    public AdminStatsService(UserMapper userMapper,
                             NoteItemMapper noteItemMapper,
                             CommentMapper commentMapper,
                             NoteFavoriteMapper noteFavoriteMapper,
                             UserFollowMapper userFollowMapper,
                             MainLinkMapper mainLinkMapper) {
        this.userMapper = userMapper;
        this.noteItemMapper = noteItemMapper;
        this.commentMapper = commentMapper;
        this.noteFavoriteMapper = noteFavoriteMapper;
        this.userFollowMapper = userFollowMapper;
        this.mainLinkMapper = mainLinkMapper;
    }

    @Cacheable(value = "stats", key = "'platformSummary'")
    public PlatformSummaryVO getPlatformSummary() {
        PlatformSummaryVO vo = new PlatformSummaryVO();

        Long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        Long totalNotes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0));
        Long markdownNotes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).eq("note_type", 2));
        Long totalType1Notes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).eq("note_type", 1));
        Long linkNotes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).eq("note_type", 1).likeRight("location_or_url", "http"));
        Long nonLinkNotes = (totalType1Notes != null ? totalType1Notes : 0L) - (linkNotes != null ? linkNotes : 0L);
        if (nonLinkNotes < 0) nonLinkNotes = 0L;

        Long totalMainLinks = mainLinkMapper.selectCount(new QueryWrapper<>());
        Long publicNotes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).eq("is_public", 1));
        Long privateNotes = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).eq("is_public", 0));

        Long totalViews = noteItemMapper.sumViewCount();
        if (totalViews == null) {
            totalViews = 0L;
        }

        Long totalComments = commentMapper.selectCount(new QueryWrapper<Comment>().eq("is_deleted", 0));
        Long totalFavorites = noteFavoriteMapper.selectCount(new QueryWrapper<>());
        Long totalFollows = userFollowMapper.selectCount(new QueryWrapper<>());

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long activeUsersToday = userMapper.selectCount(new QueryWrapper<User>().ge("last_active_at", todayStart));
        Long newNotesToday = noteItemMapper.selectCount(new QueryWrapper<NoteItem>().eq("is_deleted", 0).ge("created_at", todayStart));

        vo.setTotalUsers(totalUsers != null ? totalUsers : 0L);
        vo.setTotalNotes(totalNotes != null ? totalNotes : 0L);
        vo.setMarkdownNotes(markdownNotes != null ? markdownNotes : 0L);
        vo.setOriginalNotes(markdownNotes != null ? markdownNotes : 0L);
        vo.setLinkNotes(linkNotes != null ? linkNotes : 0L);
        vo.setNonLinkNotes(nonLinkNotes);
        vo.setTotalMainLinks(totalMainLinks != null ? totalMainLinks : 0L);
        vo.setPublicNotes(publicNotes != null ? publicNotes : 0L);
        vo.setPrivateNotes(privateNotes != null ? privateNotes : 0L);
        vo.setTotalViews(totalViews);
        vo.setTotalComments(totalComments != null ? totalComments : 0L);
        vo.setTotalFavorites(totalFavorites != null ? totalFavorites : 0L);
        vo.setTotalFollows(totalFollows != null ? totalFollows : 0L);
        vo.setActiveUsersToday(activeUsersToday != null ? activeUsersToday : 0L);
        vo.setNewNotesToday(newNotesToday != null ? newNotesToday : 0L);

        return vo;
    }

    @Cacheable(value = "stats", key = "'regTrend:' + #days")
    public List<TrendVO> getRegistrationTrend(Integer days) {
        if (days == null) days = 30;
        return buildTrend(days, "user");
    }

    @Cacheable(value = "stats", key = "'activity:' + #period")
    public List<TrendVO> getActivityStats(String period) {
        if (period == null) period = "daily";
        int days = "weekly".equals(period) ? 7 : "monthly".equals(period) ? 30 : 7;
        return buildTrend(days, "activity");
    }

    @Cacheable(value = "stats", key = "'userNoteDist'")
    public List<DistributionVO> getUserNoteDistribution() {
        List<DistributionVO> distribution = new ArrayList<>();

        String[] ranges = {"0篇", "1-5篇", "6-10篇", "11-20篇", "20篇以上"};
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

    @Cacheable(value = "stats", key = "'publishTrend:' + #days")
    public List<TrendVO> getPublishTrend(Integer days) {
        if (days == null) days = 30;
        return buildTrend(days, "note");
    }

    @Cacheable(value = "stats", key = "'viewRank:' + #limit")
    public List<NoteVO> getViewRanking(Integer limit) {
        if (limit == null) limit = 20;
        return noteItemMapper.selectTopNotes(limit);
    }

    @Cacheable(value = "stats", key = "'linkTypeStats'")
    public List<DistributionVO> getLinkTypeStats() {
        List<DistributionVO> stats = new ArrayList<>();

        // 1. Markdown 形式笔记 (note_type = 2)
        Long markdownCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .eq("note_type", 2));

        // 2. 仅链接形式笔记 (note_type = 1 且 location_or_url 以 http/https 开头)
        Long linkOnlyCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .eq("note_type", 1)
                .likeRight("location_or_url", "http"));

        // 3. 非链接形式笔记 (note_type = 1 且 location_or_url 不以 http/https 开头或为相对位置)
        Long totalType1 = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .eq("note_type", 1));

        if (totalType1 == null) totalType1 = 0L;
        if (linkOnlyCount == null) linkOnlyCount = 0L;
        Long nonLinkCount = Math.max(0L, totalType1 - linkOnlyCount);

        DistributionVO mdVo = new DistributionVO();
        mdVo.setRange("Markdown形式");
        mdVo.setCount(markdownCount != null ? markdownCount : 0L);
        stats.add(mdVo);

        DistributionVO linkVo = new DistributionVO();
        linkVo.setRange("仅链接形式");
        linkVo.setCount(linkOnlyCount);
        stats.add(linkVo);

        DistributionVO nonLinkVo = new DistributionVO();
        nonLinkVo.setRange("非链接形式");
        nonLinkVo.setCount(nonLinkCount);
        stats.add(nonLinkVo);

        return stats;
    }

    @Cacheable(value = "stats", key = "'visibilityStats'")
    public List<DistributionVO> getVisibilityStats() {
        List<DistributionVO> stats = new ArrayList<>();

        Long publicCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .eq("is_public", 1));
        Long privateCount = noteItemMapper.selectCount(new QueryWrapper<NoteItem>()
                .eq("is_deleted", 0)
                .eq("is_public", 0));

        DistributionVO pubVo = new DistributionVO();
        pubVo.setRange("公开笔记");
        pubVo.setCount(publicCount != null ? publicCount : 0L);
        stats.add(pubVo);

        DistributionVO priVo = new DistributionVO();
        priVo.setRange("私密笔记");
        priVo.setCount(privateCount != null ? privateCount : 0L);
        stats.add(priVo);

        return stats;
    }

    @Cacheable(value = "stats", key = "'interactionStats'")
    public List<DistributionVO> getInteractionStats() {
        List<DistributionVO> stats = new ArrayList<>();

        Long commentCount = commentMapper.selectCount(new QueryWrapper<Comment>().eq("is_deleted", 0));
        Long favoriteCount = noteFavoriteMapper.selectCount(new QueryWrapper<>());
        Long followCount = userFollowMapper.selectCount(new QueryWrapper<>());
        Long mainLinkCount = mainLinkMapper.selectCount(new QueryWrapper<>());

        DistributionVO cVo = new DistributionVO();
        cVo.setRange("评论互动");
        cVo.setCount(commentCount != null ? commentCount : 0L);
        stats.add(cVo);

        DistributionVO fVo = new DistributionVO();
        fVo.setRange("收藏数量");
        fVo.setCount(favoriteCount != null ? favoriteCount : 0L);
        stats.add(fVo);

        DistributionVO folVo = new DistributionVO();
        folVo.setRange("关注关系");
        folVo.setCount(followCount != null ? followCount : 0L);
        stats.add(folVo);

        DistributionVO mVo = new DistributionVO();
        mVo.setRange("知识库总链接");
        mVo.setCount(mainLinkCount != null ? mainLinkCount : 0L);
        stats.add(mVo);

        return stats;
    }

    @Cacheable(value = "stats", key = "'platformTrends:' + #days")
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
            trend.setCount(count != null ? count : 0L);
            trends.add(trend);
        }
        return trends;
    }
}
