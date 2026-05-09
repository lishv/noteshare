package com.note.scheduled;

import com.note.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    private final OperationLogMapper operationLogMapper;

    public LogCleanupTask(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldLogs() {
        try {
            int deleted = operationLogMapper.deleteOldLogs();
            log.info("清理90天前操作日志，删除{}条记录", deleted);
        } catch (Exception e) {
            log.error("清理操作日志失败", e);
        }
    }
}
