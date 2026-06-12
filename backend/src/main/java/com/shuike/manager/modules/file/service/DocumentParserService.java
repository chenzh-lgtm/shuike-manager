package com.shuike.manager.modules.file.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Slf4j
@Service
public class DocumentParserService {

    /**
     * 从文件流中提取文本内容
     */
    public String extractText(InputStream inputStream, String fileName) {
        if (fileName == null) return "";
        String lower = fileName.toLowerCase();
        try {
            if (lower.endsWith(".pdf")) {
                return extractPdfText(inputStream);
            } else if (lower.endsWith(".docx")) {
                return extractDocxText(inputStream);
            } else if (lower.endsWith(".doc")) {
                return extractDocxText(inputStream); // .doc 按 .docx 处理
            } else {
                return "[不支持的文件格式: " + fileName + "]";
            }
        } catch (Exception e) {
            log.error("[文档解析] 文件 {} 解析失败: {}", fileName, e.getMessage());
            return "[文件解析失败: " + fileName + "]";
        }
    }

    private String extractPdfText(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("[PDF解析] 提取 {} 页, {} 字符", document.getNumberOfPages(), text.length());
            return text;
        }
    }

    private String extractDocxText(InputStream inputStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> {
                String t = p.getText();
                if (t != null && !t.trim().isEmpty()) {
                    sb.append(t).append("\n");
                }
            });
            // 也提取表格内容
            doc.getTables().forEach(table -> {
                sb.append("\n[表格]\n");
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        sb.append(cell.getText()).append("\t");
                    });
                    sb.append("\n");
                });
            });
            String text = sb.toString();
            log.info("[DOCX解析] 提取 {} 字符", text.length());
            return text;
        }
    }

}
