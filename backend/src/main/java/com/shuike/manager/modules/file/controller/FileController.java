package com.shuike.manager.modules.file.controller;

import com.shuike.manager.common.config.MinioConfig;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.file.service.DocumentParserService;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterialFile;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialFileMapper;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlanFile;
import com.shuike.manager.modules.teachingplan.mapper.TeachingPlanFileMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.shuike.manager.common.aspect.OperationLog;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final PhaseMaterialFileMapper materialFileMapper;
    private final TeachingPlanFileMapper planFileMapper;
    private final DocumentParserService documentParserService;

    @OperationLog(module = "文件管理", action = "UPLOAD", targetType = "FILE")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "PLAN") String type,
                                                    @RequestParam(defaultValue = "") String materialType) {
        try {
            String originalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileType;
            String extLower = ext.toLowerCase();
            if (extLower.equals(".pdf")) fileType = "PDF";
            else if (extLower.equals(".doc") || extLower.equals(".docx")) fileType = "WORD";
            else if (extLower.equals(".jpg") || extLower.equals(".jpeg") || extLower.equals(".png")) fileType = "IMAGE";
            else fileType = "OTHER";

            // 构建 MinIO 存储路径（原文件名 + 时间戳 + 材料类型子目录）
            String baseName = originalName != null && originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf(".")) : (originalName != null ? originalName : "file");
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String timeSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String subfolder = getMaterialFolder(materialType);
            String folder = "MATERIAL".equals(type) ? "phase-materials/" + subfolder : "teaching-plans";
            String objectName = folder + "/" + datePath + "/" + baseName + "_" + timeSuffix + ext;
            String fileUrl = "/" + minioConfig.getBucket() + "/" + objectName;

            // 上传到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .stream(inputStream, fileSize, -1)
                        .contentType(file.getContentType())
                        .build());
            }
            log.info("[文件上传] 文件已存入MinIO: {}", objectName);

            // 保存数据库记录
            Long fileId;
            if ("MATERIAL".equals(type)) {
                PhaseMaterialFile f = new PhaseMaterialFile();
                f.setFileName(originalName);
                f.setFileUrl(objectName);
                f.setFileType(fileType);
                f.setFileSize(fileSize);
                materialFileMapper.insert(f);
                fileId = f.getId();
            } else {
                TeachingPlanFile f = new TeachingPlanFile();
                f.setFileName(originalName);
                f.setFileUrl(objectName);
                f.setFileType(fileType);
                f.setFileSize(fileSize);
                planFileMapper.insert(f);
                fileId = f.getId();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("fileName", originalName);
            result.put("fileUrl", fileUrl);
            result.put("fileSize", fileSize);
            result.put("storage", "MinIO");
            return ApiResponse.success("上传成功", result);

        } catch (Exception e) {
            log.error("[文件上传] 失败", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档并解析文本内容（用于人培方案、课程标准）
     * type: TALENT_PLAN / COURSE_STANDARD
     */
    @PostMapping("/upload-parse")
    public ApiResponse<Map<String, Object>> uploadAndParse(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "TALENT_PLAN") String type) {
        try {
            String originalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            // 先读取文件字节
            byte[] fileBytes = file.getBytes();
            String baseName = originalName != null && originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf(".")) : (originalName != null ? originalName : "file");
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String timeSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String folder = "COURSE_STANDARD".equals(type) ? "course-standards" : "talent-plans";
            String objectName = folder + "/" + datePath + "/" + baseName + "_" + timeSuffix + ext;

            // 上传到 MinIO
            try (InputStream is = new java.io.ByteArrayInputStream(fileBytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .stream(is, fileSize, -1)
                        .contentType(file.getContentType())
                        .build());
            }

            // 解析文档文本
            String parsedText;
            try (InputStream is = new java.io.ByteArrayInputStream(fileBytes)) {
                parsedText = documentParserService.extractText(is, originalName);
            }
            log.info("[文档上传解析] type={} file={} textLen={}", type, originalName,
                    parsedText != null ? parsedText.length() : 0);

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", originalName);
            result.put("fileUrl", objectName);
            result.put("fileSize", fileSize);
            result.put("parsedText", parsedText != null ? parsedText : "");
            result.put("storage", "MinIO");
            return ApiResponse.success("上传解析成功", result);

        } catch (Exception e) {
            log.error("[文档上传解析] 失败", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件下载/预览链接
     * mode=download(默认): attachment强制下载
     * mode=preview: inline浏览器打开 + 返回解析文本
     */
    @GetMapping("/{fileId}/download")
    public ApiResponse<Map<String, Object>> download(@PathVariable Long fileId,
                                                      @RequestParam(defaultValue = "download") String mode) {
        try {
            PhaseMaterialFile file = materialFileMapper.selectById(fileId);
            if (file == null) return ApiResponse.error(404, "文件不存在");

            boolean isPreview = "preview".equals(mode);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("response-content-disposition",
                    (isPreview ? "inline" : "attachment") + "; filename=\"" + file.getFileName() + "\"");
            // 预览模式设置MIME类型帮助浏览器渲染
            if (isPreview) {
                String lower = file.getFileName() != null ? file.getFileName().toLowerCase() : "";
                if (lower.endsWith(".pdf"))
                    queryParams.put("response-content-type", "application/pdf");
                else if (lower.endsWith(".docx"))
                    queryParams.put("response-content-type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                else if (lower.endsWith(".doc"))
                    queryParams.put("response-content-type", "application/msword");
                else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
                    queryParams.put("response-content-type", "image/jpeg");
                else if (lower.endsWith(".png"))
                    queryParams.put("response-content-type", "image/png");
            }

            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucket())
                            .object(file.getFileUrl())
                            .expiry(7200)
                            .extraQueryParams(queryParams)
                            .build());

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", file.getId());
            result.put("fileName", file.getFileName());
            result.put("fileSize", file.getFileSize());
            result.put("url", presignedUrl);
            result.put("mode", isPreview ? "preview" : "download");

            // 预览模式：同时返回解析后的文本内容供页面内展示
            if (isPreview) {
                String textContent = file.getTextContent();
                if (textContent == null || textContent.isEmpty()) {
                    try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket()).object(file.getFileUrl()).build())) {
                        textContent = documentParserService.extractText(is, file.getFileName());
                        if (textContent != null && !textContent.isEmpty()) {
                            file.setTextContent(textContent);
                            materialFileMapper.updateById(file);
                        }
                    } catch (Exception ex) {
                        log.warn("[预览] 文本解析失败: {}", file.getFileName());
                    }
                }
                if (textContent != null && !textContent.isEmpty()) {
                    result.put("textContent", textContent);
                    result.put("textLength", textContent.length());
                }
            }

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("[文件链接] 失败", e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    private String getMaterialFolder(String materialType) {
        if (materialType == null || materialType.isEmpty()) return "other";
        switch (materialType) {
            case "TEACHING_PLAN": return "teaching-plan";
            case "LESSON_PLAN":    return "lesson-plan";
            case "COURSEWARE":     return "courseware";
            case "EXAM_PLAN":      return "exam-plan";
            default:               return "other";
        }
    }

    /**
     * 删除 MinIO 文件（由 PhaseMaterialService 调用）
     */
    public void deleteFromMinio(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
            log.info("[文件删除] 已从MinIO删除: {}", objectName);
        } catch (Exception e) {
            log.warn("[文件删除] MinIO删除失败: {} err={}", objectName, e.getMessage());
        }
    }
}
