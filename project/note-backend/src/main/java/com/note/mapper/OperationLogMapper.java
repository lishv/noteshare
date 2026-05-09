package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.OperationLog;
import com.note.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    IPage<OperationLogVO> selectLogPage(Page<OperationLogVO> page,
                                        @Param("operationType") String operationType,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime);

    @Delete("DELETE FROM t_operation_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)")
    int deleteOldLogs();
}
