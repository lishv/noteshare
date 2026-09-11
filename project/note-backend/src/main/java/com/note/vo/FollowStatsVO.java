package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户关注统计数据")
public class FollowStatsVO {
    @Schema(description = "被关注数/粉丝数", example = "10")
    private Long followerCount;

    @Schema(description = "关注他人数量", example = "5")
    private Long followingCount;

    @Schema(description = "当前登录用户是否已关注该用户", example = "true")
    private Boolean isFollowing;
}
