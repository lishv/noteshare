package com.note.controller.user;

import com.note.service.UserService;
import com.note.util.AliyunOSSOperator;
import com.note.vo.Result;
import com.note.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/upload")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class UploadController {

    private final AliyunOSSOperator aliyunOSSOperator;
    private final UserService userService;

    public UploadController(AliyunOSSOperator aliyunOSSOperator, UserService userService) {
        this.aliyunOSSOperator = aliyunOSSOperator;
        this.userService = userService;
    }

    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像图片")
    public Result<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过2MB");
        }

        String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
        UserVO userVO = userService.updateAvatar(userId, url);

        return Result.success("头像上传成功", userVO);
    }

    @PostMapping("/image")
    @Operation(summary = "上传图片", description = "上传笔记插图/封面等通用图片")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过10MB");
        }

        String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
        return Result.success("图片上传成功", url);
    }
}
