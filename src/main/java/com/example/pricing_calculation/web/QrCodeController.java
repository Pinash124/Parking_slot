package com.example.pricing_calculation.web;

import com.example.pricing_calculation.service.QrCodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/qr")
@Tag(name = "QR Utility", description = "Tien ich doc va giai ma QR code tu hinh anh")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/decode")
    public ResponseEntity<Map<String, String>> decode(@RequestParam("file") MultipartFile file) {
        String decodedText = qrCodeService.decodeQrCode(file);
        return ResponseEntity.ok(Map.of("text", decodedText));
    }
}
