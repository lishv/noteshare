package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.MainLink;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.MainLinkMapper;
import com.note.mapper.OperationLogMapper;
import com.note.vo.MainLinkVO;
import com.note.vo.PageResult;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminLinkService {

    private final MainLinkMapper mainLinkMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminLinkService(MainLinkMapper mainLinkMapper, OperationLogMapper operationLogMapper) {
        this.mainLinkMapper = mainLinkMapper;
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<MainLinkVO> listLinks(Integer page, Integer size, String keyword) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<MainLinkVO> linkPage = new Page<>(page, size);
        IPage<MainLinkVO> result = mainLinkMapper.selectLinkPageAdmin(linkPage, keyword);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    @CacheEvict(value = {"dashboard", "stats"}, allEntries = true)
    public void editLink(Long linkId, String mainUrl, Long adminId, String adminUsername, String ipAddress) {
        MainLink link = mainLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND);
        }

        link.setMainUrl(mainUrl);
        link.setUpdatedAt(LocalDateTime.now());
        mainLinkMapper.updateById(link);

        logOperation(adminId, adminUsername, "LINK_MANAGE",
                "编辑链接ID: " + linkId, "MAIN_LINK", linkId, ipAddress);
    }

    @Transactional
    @CacheEvict(value = {"dashboard", "stats"}, allEntries = true)
    public void deleteLink(Long linkId, Long adminId, String adminUsername, String ipAddress) {
        MainLink link = mainLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND);
        }

        mainLinkMapper.deleteById(linkId);

        logOperation(adminId, adminUsername, "LINK_MANAGE",
                "删除链接ID: " + linkId, "MAIN_LINK", linkId, ipAddress);
    }

    @Transactional
    @CacheEvict(value = {"dashboard", "stats"}, allEntries = true)
    public void batchDeleteLinks(List<Long> linkIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long linkId : linkIds) {
            mainLinkMapper.deleteById(linkId);
        }

        logOperation(adminId, adminUsername, "LINK_MANAGE",
                "批量删除链接，数量: " + linkIds.size(), "MAIN_LINK", null, ipAddress);
    }

    private void logOperation(Long adminId, String adminUsername, String operationType,
                              String description, String targetType, Long targetId, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setOperationType(operationType);
        log.setDescription(description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
