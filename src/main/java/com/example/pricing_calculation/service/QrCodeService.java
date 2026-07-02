package com.example.pricing_calculation.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QrCodeService {

    public String decodeQrCode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tệp tin hình ảnh QR không được để trống");
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new BadRequestException("Tệp tin tải lên không phải là hình ảnh hợp lệ");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            if (result == null || result.getText() == null || result.getText().isBlank()) {
                throw new BadRequestException("Không tìm thấy nội dung mã QR trong hình ảnh");
            }

            return result.getText();
        } catch (com.google.zxing.NotFoundException e) {
            throw new BadRequestException("Không phát hiện thấy mã QR nào trong hình ảnh. Vui lòng căn chỉnh lại góc chụp.");
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi xử lý giải mã hình ảnh QR: " + e.getMessage());
        }
    }
}
