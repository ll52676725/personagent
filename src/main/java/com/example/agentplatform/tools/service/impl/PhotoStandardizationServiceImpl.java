package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.common.FileUtils;
import com.example.agentplatform.tools.dto.PhotoSizeDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationRequestDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationResultDTO;
import com.example.agentplatform.tools.service.PhotoStandardizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class PhotoStandardizationServiceImpl implements PhotoStandardizationService {
    
    private static final int DPI = 300;
    
    private static final Map<String, int[]> PHOTO_SIZES = new LinkedHashMap<>();
    
    private static final Map<String, Color> BACKGROUND_COLORS = new LinkedHashMap<>();
    
    private static final Map<String, String> BACKGROUND_COLOR_NAMES = new LinkedHashMap<>();
    
    private static final Map<String, PhotoSizeDTO> PHOTO_SIZE_INFOS = new LinkedHashMap<>();
    
    static {
        PHOTO_SIZES.put("1寸", new int[]{295, 413});
        PHOTO_SIZES.put("2寸", new int[]{413, 579});
        PHOTO_SIZES.put("小1寸", new int[]{260, 378});
        PHOTO_SIZES.put("大1寸", new int[]{390, 567});
        PHOTO_SIZES.put("身份证", new int[]{358, 441});
        
        BACKGROUND_COLORS.put("白色", new Color(255, 255, 255));
        BACKGROUND_COLORS.put("红色", new Color(255, 200, 200));
        BACKGROUND_COLORS.put("蓝色", new Color(201, 221, 255));
        
        BACKGROUND_COLOR_NAMES.put("白色", "身份证、签证、驾照等");
        BACKGROUND_COLOR_NAMES.put("红色", "社保卡、结婚证等");
        BACKGROUND_COLOR_NAMES.put("蓝色", "护照、签证、毕业证等");
        
        PHOTO_SIZE_INFOS.put("1寸", PhotoSizeDTO.builder()
                .code("1寸")
                .name("一寸")
                .widthMm("25")
                .heightMm("35")
                .widthPx(295)
                .heightPx(413)
                .description("标准一寸照片")
                .usage("身份证、驾驶证、学生证等")
                .build());
        
        PHOTO_SIZE_INFOS.put("2寸", PhotoSizeDTO.builder()
                .code("2寸")
                .name("二寸")
                .widthMm("35")
                .heightMm("49")
                .widthPx(413)
                .heightPx(579)
                .description("标准二寸照片")
                .usage("简历、毕业证书、签证等")
                .build());
        
        PHOTO_SIZE_INFOS.put("小1寸", PhotoSizeDTO.builder()
                .code("小1寸")
                .name("小一寸")
                .widthMm("22")
                .heightMm("32")
                .widthPx(260)
                .heightPx(378)
                .description("小一寸照片")
                .usage("部分考试报名、社保卡等")
                .build());
        
        PHOTO_SIZE_INFOS.put("大1寸", PhotoSizeDTO.builder()
                .code("大1寸")
                .name("大一寸")
                .widthMm("33")
                .heightMm("48")
                .widthPx(390)
                .heightPx(567)
                .description("大一寸照片")
                .usage("部分证件、公务员考试等")
                .build());
        
        PHOTO_SIZE_INFOS.put("身份证", PhotoSizeDTO.builder()
                .code("身份证")
                .name("身份证照片")
                .widthMm("26")
                .heightMm("32")
                .widthPx(358)
                .heightPx(441)
                .description("居民身份证照片")
                .usage("身份证办理")
                .build());
    }
    
    @Override
    public PhotoStandardizationResultDTO standardize(MultipartFile file, PhotoStandardizationRequestDTO request) {
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new BusinessException("无法读取图片文件");
            }

            int[] targetSize = PHOTO_SIZES.get(request.getPhotoSize());
            if (targetSize == null) {
                throw new BusinessException("不支持的照片尺寸: " + request.getPhotoSize());
            }

            Color bgColor = BACKGROUND_COLORS.get(request.getBackgroundColor());
            if (bgColor == null) {
                throw new BusinessException("不支持的背景颜色: " + request.getBackgroundColor());
            }

            int targetWidth = targetSize[0];
            int targetHeight = targetSize[1];

            BufferedImage processedImage = processImage(originalImage, targetWidth, targetHeight, bgColor);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = Boolean.TRUE.equals(request.getJpegOutput()) ? "jpg" : "png";
            String mimeType = Boolean.TRUE.equals(request.getJpegOutput()) ? "image/jpeg" : "image/png";

            int quality = request.getQuality() != null ? request.getQuality() : 90;

            if (Boolean.TRUE.equals(request.getJpegOutput())) {
                writeJpegImage(processedImage, outputStream, quality);
            } else {
                ImageIO.write(processedImage, "PNG", outputStream);
            }

            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = FileUtils.toBase64Data(imageBytes, mimeType);

            String originalFilename = file.getOriginalFilename();
            String extension = Boolean.TRUE.equals(request.getJpegOutput()) ? ".jpg" : ".png";
            String convertedFilename = originalFilename != null
                    ? originalFilename.substring(0, originalFilename.lastIndexOf('.')) + "_证件照" + extension
                    : "photo_standardized" + extension;

            return PhotoStandardizationResultDTO.builder()
                    .originalFileName(originalFilename)
                    .convertedFileName(convertedFilename)
                    .originalSize(file.getSize())
                    .convertedSize((long) imageBytes.length)
                    .width(targetWidth)
                    .height(targetHeight)
                    .photoSize(request.getPhotoSize())
                    .backgroundColor(request.getBackgroundColor())
                    .dpi(DPI + " DPI")
                    .mimeType(mimeType)
                    .imageDataBase64(base64Image)
                    .build();

        } catch (IOException e) {
            log.error("证件照处理失败", e);
            throw new BusinessException("证件照处理失败: " + e.getMessage());
        }
    }
    
    private BufferedImage processImage(BufferedImage original, int targetWidth, int targetHeight, Color bgColor) {
        BufferedImage resized = resizeImageWithPadding(original, targetWidth, targetHeight, bgColor);
        
        return replaceBackground(resized, bgColor);
    }
    
    private BufferedImage resizeImageWithPadding(BufferedImage original, int targetWidth, int targetHeight, Color bgColor) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        double widthRatio = (double) targetWidth / originalWidth;
        double heightRatio = (double) targetHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int scaledWidth = (int) (originalWidth * ratio);
        int scaledHeight = (int) (originalHeight * ratio);
        
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2d.drawImage(original.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;
        
        g2d.drawImage(scaled, x, y, null);
        g2d.dispose();
        
        return result;
    }
    
    private BufferedImage replaceBackground(BufferedImage image, Color targetColor) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(targetColor);
        g2d.fillRect(0, 0, width, height);
        
        int margin = Math.min(width, height) / 20;
        int faceRegionTop = margin;
        int faceRegionBottom = height - margin;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                
                if (y >= faceRegionTop && y <= faceRegionBottom) {
                    result.setRGB(x, y, rgb);
                } else {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    int avg = (r + g + b) / 3;
                    
                    if (avg > 240) {
                        result.setRGB(x, y, targetColor.getRGB());
                    } else {
                        result.setRGB(x, y, rgb);
                    }
                }
            }
        }
        
        g2d.dispose();
        return result;
    }
    
    private void writeJpegImage(BufferedImage image, ByteArrayOutputStream outputStream, int quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", outputStream);
            return;
        }
        
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);
        
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        
        writer.dispose();
        ios.close();
    }
    
    @Override
    public List<PhotoSizeDTO> getSupportedPhotoSizes() {
        return new ArrayList<>(PHOTO_SIZE_INFOS.values());
    }
    
    @Override
    public List<String> getSupportedBackgroundColors() {
        return new ArrayList<>(BACKGROUND_COLORS.keySet());
    }
}
