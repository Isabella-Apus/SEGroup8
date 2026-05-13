package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.config.UploadProperties;
import com.segroup8.platform.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final UploadProperties uploadProperties;

    public FileServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file, true);
        return store(file);
    }

    @Override
    public String uploadMedia(MultipartFile file) {
        validateFile(file, false);
        return store(file);
    }

    private void validateFile(MultipartFile file, boolean imageOnly) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(400, "无法识别文件类型");
        }
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        boolean allowed = normalizedType.startsWith("image/")
                || (!imageOnly && normalizedType.startsWith("video/"));
        if (!allowed) {
            throw new BusinessException(400, imageOnly ? "请上传图片文件" : "仅支持上传图片或视频");
        }
    }

    private String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String filename = UUID.randomUUID() + (ext == null ? "" : "." + ext);

        try {
            Path uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath();
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new BusinessException(500, "文件上传失败");
        }
    }
}
