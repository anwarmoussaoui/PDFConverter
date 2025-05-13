package com.example.PDFconverter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class UploadController {

    private final PDFService pdfService;

    public UploadController(PDFService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        return "upload"; // Thymeleaf template name
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] excelBytes = file.getBytes();
        byte[] pdfBytes = pdfService.toPDF(excelBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    @PostMapping("/extract")
    public void extract(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] pdfFile = Files.readAllBytes(Paths.get("output.pdf"));
        pdfService.fromPDF(pdfFile);

    }
    @GetMapping("/extract")
    public String extractPage(Model model) {
        return "extract"; // Thymeleaf template name
    }
}
