package com.note.service;

import com.note.entity.MainLink;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.MainLinkMapper;
import com.note.vo.MainLinkVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MainLinkService {

    private final MainLinkMapper mainLinkMapper;

    public MainLinkService(MainLinkMapper mainLinkMapper) {
        this.mainLinkMapper = mainLinkMapper;
    }

    @Transactional
    public MainLinkVO setMainLink(Long userId, String mainUrl) {
        MainLink existingLink = mainLinkMapper.selectByUserId(userId);

        if (existingLink != null) {
            existingLink.setMainUrl(mainUrl);
            existingLink.setUpdatedAt(LocalDateTime.now());
            mainLinkMapper.updateById(existingLink);
            return convertToVO(existingLink);
        } else {
            MainLink mainLink = new MainLink();
            mainLink.setUserId(userId);
            mainLink.setMainUrl(mainUrl);
            mainLink.setViewCount(0L);
            mainLink.setCreatedAt(LocalDateTime.now());
            mainLink.setUpdatedAt(LocalDateTime.now());
            mainLinkMapper.insert(mainLink);
            return convertToVO(mainLink);
        }
    }

    public MainLinkVO getMainLinkByUserId(Long userId) {
        MainLink mainLink = mainLinkMapper.selectByUserId(userId);
        if (mainLink == null) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND);
        }

        mainLinkMapper.incrementViewCount(mainLink.getId());
        mainLink.setViewCount(mainLink.getViewCount() + 1);

        return convertToVO(mainLink);
    }

    public MainLinkVO getMyMainLink(Long userId) {
        MainLink mainLink = mainLinkMapper.selectByUserId(userId);
        if (mainLink == null) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND);
        }
        return convertToVO(mainLink);
    }

    @Transactional
    public void deleteMainLink(Long userId) {
        MainLink mainLink = mainLinkMapper.selectByUserId(userId);
        if (mainLink == null) {
            throw new BusinessException(ErrorCode.MAIN_LINK_NOT_FOUND);
        }
        mainLinkMapper.deleteById(mainLink.getId());
    }

    public boolean hasMainLink(Long userId) {
        return mainLinkMapper.countByUserId(userId) > 0;
    }

    private MainLinkVO convertToVO(MainLink mainLink) {
        MainLinkVO vo = new MainLinkVO();
        vo.setId(mainLink.getId());
        vo.setUserId(mainLink.getUserId());
        vo.setMainUrl(mainLink.getMainUrl());
        vo.setViewCount(mainLink.getViewCount());
        vo.setCreatedAt(mainLink.getCreatedAt());
        vo.setUpdatedAt(mainLink.getUpdatedAt());
        return vo;
    }
}
