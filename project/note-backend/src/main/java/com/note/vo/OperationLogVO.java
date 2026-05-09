package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志响应数据")
public class OperationLogVO {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作管理员用户名")
    private String adminUsername;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "操作对象类型")
    private String targetType;

    @Schema(description = "操作对象ID")
    private Long targetId;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "操作时间")
    private LocalDateTime createdAt;
}
