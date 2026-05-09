package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.mapper.OperationLogMapper;
import com.note.vo.OperationLogVO;
import com.note.vo.PageResult;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationLogService {

    private final OperationLogMapper operationLogMapper;

    public AdminOperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<OperationLogVO> listLogs(Integer page, Integer size,
                                                String operationType, String startTime, String endTime) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<OperationLogVO> logPage = new Page<>(page, size);
        IPage<OperationLogVO> result = operationLogMapper.selectLogPage(logPage, operationType, startTime, endTime);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }
}
