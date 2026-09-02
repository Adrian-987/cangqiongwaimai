package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    public Result<String> updatePicture(MultipartFile file) throws IOException {
        log.info("传入的图片是{}",file);
        String originalName= file.getOriginalFilename();
        String string=originalName.substring(originalName.lastIndexOf("."));
        String name= UUID.randomUUID().toString()+string;
        String str=aliOssUtil.upload(file.getBytes(),name);
        return Result.success(str);
    }
}
