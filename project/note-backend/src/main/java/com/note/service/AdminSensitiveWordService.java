package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.dto.SensitiveWordBatchDTO;
import com.note.dto.SensitiveWordDTO;
import com.note.entity.OperationLog;
import com.note.entity.SensitiveWord;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.OperationLogMapper;
import com.note.mapper.SensitiveWordMapper;
import com.note.vo.PageResult;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminSensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordService sensitiveWordService;
    private final OperationLogMapper operationLogMapper;

    public AdminSensitiveWordService(SensitiveWordMapper sensitiveWordMapper,
                                     SensitiveWordService sensitiveWordService,
                                     OperationLogMapper operationLogMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<SensitiveWord> listWords(Integer page, Integer size, String keyword, Integer type, Integer status) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SensitiveWord::getWord, keyword);
        }
        if (type != null) {
            wrapper.eq(SensitiveWord::getType, type);
        }
        if (status != null) {
            wrapper.eq(SensitiveWord::getStatus, status);
        }
        wrapper.orderByDesc(SensitiveWord::getCreatedAt);

        Page<SensitiveWord> wordPage = new Page<>(page, size);
        IPage<SensitiveWord> result = sensitiveWordMapper.selectPage(wordPage, wrapper);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public SensitiveWord getWord(Long id) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_EXISTS, "敏感词不存在");
        }
        return word;
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void createWord(SensitiveWordDTO dto, Long adminId, String adminUsername, String ipAddress) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getWord, dto.getWord());
        if (sensitiveWordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_EXISTS);
        }

        SensitiveWord word = new SensitiveWord();
        word.setWord(dto.getWord());
        word.setType(dto.getType() != null ? dto.getType() : 1);
        word.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        word.setCreatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(word);

        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "新增敏感词: " + dto.getWord(), "SENSITIVE_WORD", word.getId(), ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void updateWord(Long id, SensitiveWordDTO dto, Long adminId, String adminUsername, String ipAddress) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_EXISTS, "敏感词不存在");
        }

        if (dto.getWord() != null) word.setWord(dto.getWord());
        if (dto.getType() != null) word.setType(dto.getType());
        if (dto.getStatus() != null) word.setStatus(dto.getStatus());
        sensitiveWordMapper.updateById(word);

        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "编辑敏感词ID: " + id, "SENSITIVE_WORD", id, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void deleteWord(Long id, Long adminId, String adminUsername, String ipAddress) {
        sensitiveWordMapper.deleteById(id);
        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "删除敏感词ID: " + id, "SENSITIVE_WORD", id, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void toggleWordStatus(Long id, Integer status, Long adminId, String adminUsername, String ipAddress) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_EXISTS, "敏感词不存在");
        }
        word.setStatus(status);
        sensitiveWordMapper.updateById(word);
        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                (status == 1 ? "启用" : "禁用") + "敏感词ID: " + id, "SENSITIVE_WORD", id, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void batchImport(SensitiveWordBatchDTO dto, Long adminId, String adminUsername, String ipAddress) {
        int type = dto.getType() != null ? dto.getType() : 1;
        for (String w : dto.getWords()) {
            LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensitiveWord::getWord, w);
            if (sensitiveWordMapper.selectCount(wrapper) == 0) {
                SensitiveWord word = new SensitiveWord();
                word.setWord(w);
                word.setType(type);
                word.setStatus(1);
                word.setCreatedAt(LocalDateTime.now());
                sensitiveWordMapper.insert(word);
            }
        }

        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "批量导入敏感词，数量: " + dto.getWords().size(), "SENSITIVE_WORD", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void batchDelete(List<Long> ids, Long adminId, String adminUsername, String ipAddress) {
        sensitiveWordMapper.deleteBatchIds(ids);
        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "批量删除敏感词，数量: " + ids.size(), "SENSITIVE_WORD", null, ipAddress);
    }

    @Transactional
    @CacheEvict(value = "stats", allEntries = true)
    public void batchToggleStatus(List<Long> ids, Integer status, Long adminId, String adminUsername, String ipAddress) {
        for (Long id : ids) {
            SensitiveWord word = sensitiveWordMapper.selectById(id);
            if (word != null) {
                word.setStatus(status);
                sensitiveWordMapper.updateById(word);
            }
        }
        sensitiveWordService.refreshCache();

        logOperation(adminId, adminUsername, "WORD_MANAGE",
                "批量" + (status == 1 ? "启用" : "禁用") + "敏感词，数量: " + ids.size(),
                "SENSITIVE_WORD", null, ipAddress);
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
