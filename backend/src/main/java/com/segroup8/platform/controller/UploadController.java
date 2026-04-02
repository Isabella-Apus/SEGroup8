package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String path = fileService.uploadImage(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", path);
        data.put("filename", StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "");
        return Result.success(data);
    }
}
