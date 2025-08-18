package com.mumuk.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class ImagePreprocessingUtil {

    /**
     * 전처리가 필요한지 판단
     */
    public boolean needsPreprocessing(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            ImageQualityResult qualityResult = analyzeImageQuality(imageBytes);
            return qualityResult.hasAnyIssue();
        } catch (IOException e) {
            log.error("❌ 전처리 필요 여부 판단 실패", e);
            return true; // 안전하게 전처리 필요하다고 처리
        }
    }

    /**
     * OCR을 위한 이미지 전처리
     */
    public byte[] preprocessForOcr(MultipartFile imageFile) {
        try {
            byte[] originalBytes = imageFile.getBytes();
            return preprocessImage(originalBytes);
        } catch (IOException e) {
            log.error("❌ OCR 전처리 실패", e);
            try {
                return imageFile.getBytes(); // 실패시 원본 반환
            } catch (IOException ex) {
                throw new RuntimeException("이미지 처리 실패", ex);
            }
        }
    }

    /**
     * 이미지 품질 분석
     */
    public ImageQualityResult analyzeImageQuality(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            boolean sizeIssue = image.getWidth() < 300 || image.getHeight() < 300;
            boolean contrastIssue = hasLowContrast(image);
            
            log.info("📊 이미지 품질 분석: 크기문제={}, 대비문제={}", sizeIssue, contrastIssue);
            
            return new ImageQualityResult(sizeIssue, contrastIssue);
            
        } catch (IOException e) {
            log.error("❌ 이미지 품질 분석 실패", e);
            return new ImageQualityResult(true, true); // 안전하게 문제 있다고 처리
        }
    }

    /**
     * 이미지 전처리 (품질 개선)
     */
    public byte[] preprocessImage(byte[] originalImageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImageBytes));
            
            // 해상도 개선
            BufferedImage enhanced = enhanceResolution(image);
            
            // 대비 개선
            enhanced = enhanceContrast(enhanced);
            
            // 노이즈 제거
            enhanced = removeNoise(enhanced);
            
            // 결과를 byte array로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(enhanced, "jpg", baos);
            
            log.info("✅ 이미지 전처리 완료");
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("❌ 이미지 전처리 실패, 원본 반환", e);
            return originalImageBytes; // 실패시 원본 반환
        }
    }

    private boolean hasLowContrast(BufferedImage image) {
        // 간단한 대비 검사 로직
        int[] histogram = new int[256];
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 
                                 0.587 * ((rgb >> 8) & 0xFF) + 
                                 0.114 * (rgb & 0xFF));
                histogram[gray]++;
            }
        }
        
        // 히스토그램 분산을 통한 대비 판단
        double mean = 127.5;
        double variance = 0;
        int totalPixels = image.getWidth() * image.getHeight();
        
        for (int i = 0; i < 256; i++) {
            double probability = (double) histogram[i] / totalPixels;
            variance += probability * Math.pow(i - mean, 2);
        }
        
        return variance < 1000; // 임계값은 조정 가능
    }

    private BufferedImage enhanceResolution(BufferedImage image) {
        // 최소 해상도 보장
        int minWidth = 800;
        int minHeight = 600;
        
        if (image.getWidth() < minWidth || image.getHeight() < minHeight) {
            double scaleX = (double) minWidth / image.getWidth();
            double scaleY = (double) minHeight / image.getHeight();
            double scale = Math.max(scaleX, scaleY);
            
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            
            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            return scaledImage;
        }
        
        return image;
    }

    private BufferedImage enhanceContrast(BufferedImage image) {
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // 간단한 대비 향상 (gamma correction)
                r = (int) Math.min(255, Math.pow(r / 255.0, 0.8) * 255);
                g = (int) Math.min(255, Math.pow(g / 255.0, 0.8) * 255);
                b = (int) Math.min(255, Math.pow(b / 255.0, 0.8) * 255);
                
                enhanced.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        return enhanced;
    }

    private BufferedImage removeNoise(BufferedImage image) {
        // 간단한 블러 필터로 노이즈 제거
        BufferedImage denoised = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                int totalR = 0, totalG = 0, totalB = 0;
                
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int rgb = image.getRGB(x + dx, y + dy);
                        totalR += (rgb >> 16) & 0xFF;
                        totalG += (rgb >> 8) & 0xFF;
                        totalB += rgb & 0xFF;
                    }
                }
                
                int avgR = totalR / 9;
                int avgG = totalG / 9;
                int avgB = totalB / 9;
                
                denoised.setRGB(x, y, (avgR << 16) | (avgG << 8) | avgB);
            }
        }
        
        return denoised;
    }

    public static class ImageQualityResult {
        private final boolean sizeIssue;
        private final boolean contrastIssue;
        
        public ImageQualityResult(boolean sizeIssue, boolean contrastIssue) {
            this.sizeIssue = sizeIssue;
            this.contrastIssue = contrastIssue;
        }
        
        public boolean hasSizeIssue() {
            return sizeIssue;
        }
        
        public boolean hasContrastIssue() {
            return contrastIssue;
        }
        
        public boolean hasAnyIssue() {
            return sizeIssue || contrastIssue;
        }
    }
}
