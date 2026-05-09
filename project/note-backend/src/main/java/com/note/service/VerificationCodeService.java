package com.note.service;

import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.util.VerificationCodeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MailService mailService;

    @Value("${verification.code.expire-minutes:5}")
    private int expireMinutes;

    @Value("${verification.code.send-interval:60}")
    private int sendInterval;

    @Value("${verification.code.max-daily-count:10}")
    private int maxDailyCount;

    private static final String CODE_KEY_PREFIX = "verify:code:";
    private static final String INTERVAL_KEY_PREFIX = "verify:interval:";
    private static final String DAILY_COUNT_KEY_PREFIX = "verify:daily:";

    public VerificationCodeService(RedisTemplate<String, Object> redisTemplate, MailService mailService) {
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
    }

    public void sendCode(String email, String scene) {
        String intervalKey = INTERVAL_KEY_PREFIX + scene + ":" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(intervalKey))) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_SEND_TOO_FREQUENT);
        }

        String dailyKey = DAILY_COUNT_KEY_PREFIX + scene + ":" + email;
        Integer count = (Integer) redisTemplate.opsForValue().get(dailyKey);
        if (count != null && count >= maxDailyCount) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_DAILY_LIMIT);
        }

        String code = VerificationCodeUtil.generate(6);
        String codeKey = CODE_KEY_PREFIX + scene + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, expireMinutes, TimeUnit.MINUTES);

        redisTemplate.opsForValue().set(intervalKey, 1, sendInterval, TimeUnit.SECONDS);

        if (count == null) {
            redisTemplate.opsForValue().set(dailyKey, 1, 1, TimeUnit.DAYS);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }

        mailService.sendVerificationCode(email, code);
    }

    public void verifyCode(String email, String scene, String inputCode) {
        if (inputCode == null || inputCode.isEmpty()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_REQUIRED);
        }

        String codeKey = CODE_KEY_PREFIX + scene + ":" + email;
        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!inputCode.equals(storedCode.toString())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        redisTemplate.delete(codeKey);
    }
}
