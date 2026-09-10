package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/common")
public class CommonController {
    /** 允许上传的图片后缀白名单 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    /** 上传文件大小上限：5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    @Autowired
    AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    public Result<String> updatePicture(MultipartFile file) throws IOException {
        log.info("传入的图片是{}",file);
        //1、文件不能为空
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        //2、大小校验，防止超大文件占用带宽与存储
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(MessageConstant.FILE_TOO_LARGE);
        }
        //3、文件名与后缀校验：原代码直接 substring，没有后缀时会抛 StringIndexOutOfBoundsException
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.lastIndexOf(".") < 0) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }
        String extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }
        //4、统一用 UUID 重命名，避免用户原始文件名带来的覆盖/特殊字符问题
        String name = UUID.randomUUID().toString() + "." + extension;
        String str = aliOssUtil.upload(file.getBytes(), name);
        return Result.success(str);
    }
}
