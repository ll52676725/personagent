package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.dto.ImageConvertRequestDTO;
import com.example.agentplatform.tools.dto.ImageConvertResultDTO;
import com.example.agentplatform.tools.dto.ImageFormatInfoDTO;
import com.example.agentplatform.tools.service.ImageConvertService;
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
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class ImageConvertServiceImpl implements ImageConvertService {

    private static final Map<String, String> FORMAT_MIME_MAP = new LinkedHashMap<>();
    private static final Map<String, String> FORMAT_DESCRIPTION_MAP = new LinkedHashMap<>();
    private static final Set<String> LOSSY_FORMATS = Set.of("jpg", "jpeg", "webp");
    private static final Set<String> IMAGE4J_FORMATS = Set.of("ico");

    static {
        FORMAT_MIME_MAP.put("png", "image/png");
        FORMAT_MIME_MAP.put("jpg", "image/jpeg");
        FORMAT_MIME_MAP.put("jpeg", "image/jpeg");
        FORMAT_MIME_MAP.put("gif", "image/gif");
        FORMAT_MIME_MAP.put("bmp", "image/bmp");
        FORMAT_MIME_MAP.put("tiff", "image/tiff");
        FORMAT_MIME_MAP.put("tif", "image/tiff");
        FORMAT_MIME_MAP.put("webp", "image/webp");
        FORMAT_MIME_MAP.put("ico", "image/x-icon");
        FORMAT_MIME_MAP.put("pcx", "image/x-pcx");
        FORMAT_MIME_MAP.put("pnm", "image/x-portable-anymap");
        FORMAT_MIME_MAP.put("tga", "image/x-tga");
        FORMAT_MIME_MAP.put("psd", "image/vnd.adobe.photoshop");
        FORMAT_MIME_MAP.put("sgi", "image/x-sgi");

        FORMAT_DESCRIPTION_MAP.put("png", "PNG - 便携式网络图形（无损压缩）");
        FORMAT_DESCRIPTION_MAP.put("jpg", "JPG - 联合图像专家组（有损压缩）");
        FORMAT_DESCRIPTION_MAP.put("jpeg", "JPEG - 联合图像专家组（有损压缩）");
        FORMAT_DESCRIPTION_MAP.put("gif", "GIF - 图形交换格式（支持动画）");
        FORMAT_DESCRIPTION_MAP.put("bmp", "BMP - 位图格式（无压缩）");
        FORMAT_DESCRIPTION_MAP.put("tiff", "TIFF - 标记图像文件格式（高质量）");
        FORMAT_DESCRIPTION_MAP.put("tif", "TIF - 标记图像文件格式（高质量）");
        FORMAT_DESCRIPTION_MAP.put("webp", "WebP - 现代网络图像格式（高效压缩）");
        FORMAT_DESCRIPTION_MAP.put("ico", "ICO - Windows 图标格式");
        FORMAT_DESCRIPTION_MAP.put("pcx", "PCX - PC 画笔格式");
        FORMAT_DESCRIPTION_MAP.put("pnm", "PNM - 便携式任意映射格式");
        FORMAT_DESCRIPTION_MAP.put("tga", "TGA - Targa 图像格式");
        FORMAT_DESCRIPTION_MAP.put("psd", "PSD - Adobe Photoshop 格式（仅读取）");
        FORMAT_DESCRIPTION_MAP.put("sgi", "SGI - Silicon Graphics 图像格式");
    }

    @Override
    public ImageConvertResultDTO convert(MultipartFile file, ImageConvertRequestDTO request) {
        String targetFormat = request.getTargetFormat().toLowerCase().trim();
        validateTargetFormat(targetFormat);

        byte[] convertedBytes = convertToBytesInternal(file, request);

        BufferedImage originalImage = readImage(file);
        String sourceFormat = detectSourceFormat(file.getOriginalFilename());
        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String convertedFileName = buildConvertedFileName(originalFileName, targetFormat);

        String base64Data = "data:" + FORMAT_MIME_MAP.get(targetFormat) + ";base64," +
                Base64.getEncoder().encodeToString(convertedBytes);

        log.info("图片格式转换完成: {} -> {}, 原始大小: {} bytes, 转换后大小: {} bytes",
                sourceFormat, targetFormat, file.getSize(), convertedBytes.length);

        return ImageConvertResultDTO.builder()
                .originalFormat(sourceFormat)
                .targetFormat(targetFormat)
                .originalFileName(originalFileName)
                .convertedFileName(convertedFileName)
                .originalSize(file.getSize())
                .convertedSize((long) convertedBytes.length)
                .width(originalImage.getWidth())
                .height(originalImage.getHeight())
                .mimeType(FORMAT_MIME_MAP.get(targetFormat))
                .imageDataBase64(base64Data)
                .rawImageData(convertedBytes)
                .build();
    }

    private byte[] convertToBytesInternal(MultipartFile file, ImageConvertRequestDTO request) {
        String targetFormat = request.getTargetFormat().toLowerCase().trim();
        validateTargetFormat(targetFormat);

        BufferedImage originalImage = readImage(file);

        if (request.getWidth() != null && request.getHeight() != null) {
            originalImage = resizeImage(originalImage, request.getWidth(), request.getHeight());
        }

        float quality = request.getQuality() != null ? request.getQuality() : 0.85f;
        quality = Math.max(0.1f, Math.min(1.0f, quality));

        BufferedImage processedImage = prepareForTargetFormat(originalImage, targetFormat);

        return writeImage(processedImage, targetFormat, quality);
    }

    @Override
    public List<ImageFormatInfoDTO> getSupportedFormats() {
        String[] readerFormats = ImageIO.getReaderFormatNames();
        String[] writerFormats = ImageIO.getWriterFormatNames();

        Set<String> readerSet = new HashSet<>(Arrays.asList(readerFormats));
        Set<String> writerSet = new HashSet<>(Arrays.asList(writerFormats));

        Map<String, ImageFormatInfoDTO> formatMap = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : FORMAT_MIME_MAP.entrySet()) {
            String formatKey = entry.getKey();
            String lowerKey = formatKey.toLowerCase();

            boolean readable = readerSet.stream().anyMatch(r -> r.equalsIgnoreCase(formatKey));
            boolean writable = writerSet.stream().anyMatch(w -> w.equalsIgnoreCase(formatKey));

            if (IMAGE4J_FORMATS.contains(formatKey.toLowerCase())) {
                writable = true;
            }

            if (formatMap.containsKey(lowerKey) && lowerKey.equals("jpeg")) {
                continue;
            }

            List<String> extensions = new ArrayList<>();
            extensions.add(formatKey);
            if (formatKey.equals("jpg")) {
                extensions.add("jpeg");
            } else if (formatKey.equals("jpeg")) {
                extensions.add("jpg");
            } else if (formatKey.equals("tiff")) {
                extensions.add("tif");
            } else if (formatKey.equals("tif")) {
                extensions.add("tiff");
            }

            String displayFormat = formatKey;
            if (formatKey.equals("jpg") || formatKey.equals("jpeg")) {
                displayFormat = "jpg";
            } else if (formatKey.equals("tiff") || formatKey.equals("tif")) {
                displayFormat = "tiff";
            }

            if (formatMap.containsKey(displayFormat)) {
                continue;
            }

            formatMap.put(displayFormat, ImageFormatInfoDTO.builder()
                    .formatName(displayFormat)
                    .extensions(extensions)
                    .mimeType(entry.getValue())
                    .readable(readable)
                    .writable(writable)
                    .description(FORMAT_DESCRIPTION_MAP.getOrDefault(formatKey, displayFormat))
                    .build());
        }

        return new ArrayList<>(formatMap.values());
    }

    private void validateTargetFormat(String targetFormat) {
        if (IMAGE4J_FORMATS.contains(targetFormat.toLowerCase())) {
            return;
        }
        String[] writerFormats = ImageIO.getWriterFormatNames();
        boolean supported = Arrays.stream(writerFormats)
                .anyMatch(f -> f.equalsIgnoreCase(targetFormat));
        if (!supported) {
            throw new BusinessException("不支持的目标格式: " + targetFormat);
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BusinessException("无法读取图片文件，请确认文件格式正确");
            }
            return image;
        } catch (IOException e) {
            log.error("读取图片失败", e);
            throw new BusinessException("读取图片文件失败: " + e.getMessage());
        }
    }

    private String detectSourceFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "unknown";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        return resized;
    }

    private BufferedImage prepareForTargetFormat(BufferedImage image, String targetFormat) {
        if (LOSSY_FORMATS.contains(targetFormat) && image.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            try {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                g2d.drawImage(image, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            return rgbImage;
        }
        return image;
    }

    private byte[] writeImage(BufferedImage image, String formatName, float quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if (IMAGE4J_FORMATS.contains(formatName)) {
                writeWithImage4j(image, formatName, baos);
            } else if (LOSSY_FORMATS.contains(formatName)) {
                writeWithQuality(image, formatName, quality, baos);
            } else {
                boolean written = ImageIO.write(image, formatName, baos);
                if (!written) {
                    throw new BusinessException("无法将图片写入 " + formatName + " 格式");
                }
            }
        } catch (IOException e) {
            log.error("写入图片失败", e);
            throw new BusinessException("图片格式转换失败: " + e.getMessage());
        }
        return baos.toByteArray();
    }

    private void writeWithImage4j(BufferedImage image, String formatName,
                                   ByteArrayOutputStream baos) throws IOException {
        if ("ico".equalsIgnoreCase(formatName)) {
            BufferedImage iconImage = prepareIconImage(image);
            com.xqlee.image.image4j.codec.ico.ICOEncoder.write(iconImage, baos);
        } else {
            throw new BusinessException("不支持的 image4j 格式: " + formatName);
        }
    }

    private BufferedImage prepareIconImage(BufferedImage image) {
        int[] standardSizes = {256, 128, 64, 48, 32, 16};
        int targetSize = 32;
        for (int size : standardSizes) {
            if (image.getWidth() >= size || image.getHeight() >= size) {
                targetSize = size;
                break;
            }
        }
        if (image.getWidth() == targetSize && image.getHeight() == targetSize) {
            return image;
        }
        return resizeImage(image, targetSize, targetSize);
    }

    private void writeWithQuality(BufferedImage image, String formatName, float quality,
                                  ByteArrayOutputStream baos) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new BusinessException("找不到 " + formatName + " 格式的编码器");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private String buildConvertedFileName(String originalFileName, String targetFormat) {
        int dotIndex = originalFileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? originalFileName.substring(0, dotIndex) : originalFileName;
        return baseName + "." + targetFormat;
    }
}
