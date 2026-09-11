package com.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USERNAME_DUPLICATE(1003, "用户名已存在"),
    EMAIL_DUPLICATE(1004, "邮箱已被使用"),
    PASSWORD_ERROR(1005, "密码错误"),
    ACCOUNT_LOCKED(1006, "账号已锁定，请10分钟后再试"),
    TOKEN_EXPIRED(1007, "Token已过期"),
    TOKEN_INVALID(1008, "Token无效"),
    BIO_CONTAINS_SENSITIVE_WORD(1009, "个性签名包含敏感词"),

    NOTE_NOT_FOUND(2001, "笔记不存在"),
    NOTE_ACCESS_DENIED(2002, "无权限访问该笔记"),
    NOTE_CONTAINS_SENSITIVE_WORD(2003, "笔记包含敏感词"),
    NOTE_DELETED(2004, "笔记已删除"),
    COMMENT_NOT_FOUND(2005, "评论不存在"),
    COMMENT_CONTAINS_SENSITIVE_WORD(2006, "评论包含敏感词"),

    MAIN_LINK_NOT_FOUND(3001, "总链接不存在"),
    MAIN_LINK_ACCESS_DENIED(3002, "无权限访问该总链接"),

    EMAIL_FORMAT_ERROR(4001, "邮箱格式错误"),
    PHONE_FORMAT_ERROR(4002, "手机号格式错误"),
    PASSWORD_TOO_WEAK(4003, "密码强度不足"),
    URL_FORMAT_ERROR(4004, "URL格式错误"),

    VERIFICATION_CODE_EXPIRED(5001, "验证码已过期"),
    VERIFICATION_CODE_ERROR(5002, "验证码错误"),
    VERIFICATION_CODE_SEND_TOO_FREQUENT(5003, "验证码发送过于频繁，请稍后再试"),
    VERIFICATION_CODE_DAILY_LIMIT(5004, "验证码发送次数已达每日上限"),
    VERIFICATION_CODE_REQUIRED(5005, "请输入验证码"),
    EMAIL_SEND_FAILED(5006, "邮件发送失败，请稍后重试"),

    // 管理端错误码 6xxx
    ADMIN_NOT_FOUND(6001, "管理员不存在"),
    ADMIN_USERNAME_DUPLICATE(6002, "管理员用户名已存在"),
    ADMIN_PASSWORD_ERROR(6003, "管理员密码错误"),
    ADMIN_ACCOUNT_LOCKED(6004, "管理员账号已锁定"),
    ADMIN_ACCOUNT_DISABLED(6005, "管理员账号已禁用"),
    ADMIN_ACCESS_DENIED(6006, "无管理端访问权限"),
    ADMIN_SUPER_REQUIRED(6007, "仅超级管理员可执行此操作"),
    ADMIN_LAST_SUPER(6008, "不能禁用最后一个超级管理员"),
    SENSITIVE_WORD_EXISTS(6009, "敏感词已存在"),
    BATCH_PARTIAL_FAIL(6010, "批量操作部分失败"),
    CANNOT_OPERATE_SELF(6011, "不能操作自己的账号");

    private final int code;
    private final String message;
}
