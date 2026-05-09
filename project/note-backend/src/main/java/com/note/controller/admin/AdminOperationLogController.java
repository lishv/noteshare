package com.note.controller.admin;

import com.note.service.AdminOperationLogService;
import com.note.vo.OperationLogVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/logs")
@Tag(name = "管理端-操作日志", description = "管理端操作日志查看")
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    public AdminOperationLogController(AdminOperationLogService adminOperationLogService) {
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    @Operation(summary = "操作日志列表", description = "分页获取操作日志列表")
    public Result<PageResult<OperationLogVO>> listLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        return Result.success(adminOperationLogService.listLogs(page, size, operationType, startTime, endTime));
    }
}
