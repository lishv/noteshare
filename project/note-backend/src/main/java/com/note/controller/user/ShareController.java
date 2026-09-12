package com.note.controller.user;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/user/share")
@Tag(name = "分享工具", description = "分享内容相关工具接口")
public class ShareController {

    private static final int MAX_QR_TEXT_LENGTH = 512;

    @GetMapping(value = "/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "生成分享二维码", description = "生成可扫描的笔记分享链接二维码")
    public ResponseEntity<byte[]> generateQrCode(
            @Parameter(description = "需要编码的分享链接") @RequestParam("text") String text) {
        String value = text == null ? "" : text.trim();
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (value.isEmpty()
                || value.length() > MAX_QR_TEXT_LENGTH
                || (!lowerValue.startsWith("http://") && !lowerValue.startsWith("https://"))
                || value.getBytes(StandardCharsets.UTF_8).length > MAX_QR_TEXT_LENGTH * 4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            QrConfig config = QrConfig.create()
                    .setWidth(360)
                    .setHeight(360)
                    .setMargin(2)
                    .setCharset(StandardCharsets.UTF_8);
            byte[] png = QrCodeUtil.generatePng(value, config);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.noCache())
                    .body(png);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
