package com.shiyq.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * 图像格式转换工具类
 * 支持将各种图像格式转换为ICO格式
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Slf4j
public class ImageConverterUtil {

    /**
     * 将图像文件转换为ICO格式
     *
     * @param sourceFile 源图像文件
     * @param targetFile 目标ICO文件
     * @param size       ICO图标尺寸（建议16, 32, 48, 64, 128, 256）
     * @throws IOException 转换过程中的IO异常
     */
    public static void convertToIco(File sourceFile, File targetFile, int size) throws IOException {
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("源文件不存在: " + sourceFile.getAbsolutePath());
        }

        // 读取源图像
        BufferedImage sourceImage = ImageIO.read(sourceFile);
        if (sourceImage == null) {
            throw new IOException("无法读取图像文件: " + sourceFile.getAbsolutePath());
        }

        // 创建目标目录
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 转换为ICO格式
        convertToIco(sourceImage, targetFile, size);
        
        log.info("成功将图像转换为ICO格式: {} -> {}", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
    }

    /**
     * 将BufferedImage转换为ICO格式
     *
     * @param sourceImage 源图像
     * @param targetFile  目标ICO文件
     * @param size        ICO图标尺寸
     * @throws IOException 转换过程中的IO异常
     */
    public static void convertToIco(BufferedImage sourceImage, File targetFile, int size) throws IOException {
        // 调整图像尺寸
        BufferedImage resizedImage = resizeImage(sourceImage, size, size);
        
        // 写入ICO文件
        writeIcoFile(resizedImage, targetFile);
    }

    /**
     * 调整图像尺寸
     *
     * @param originalImage 原始图像
     * @param width         目标宽度
     * @param height        目标高度
     * @return 调整后的图像
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // 设置高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        
        return resizedImage;
    }

    /**
     * 写入ICO文件
     * ICO文件格式规范实现
     *
     * @param image      图像数据
     * @param targetFile 目标文件
     * @throws IOException IO异常
     */
    private static void writeIcoFile(BufferedImage image, File targetFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             DataOutputStream dos = new DataOutputStream(fos)) {
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // 将图像转换为PNG格式的字节数组
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", pngStream);
            byte[] pngData = pngStream.toByteArray();
            
            // ICO文件头 (6 bytes)
            writeLittleEndianShort(dos, 0); // Reserved (2 bytes)
            writeLittleEndianShort(dos, 1); // Type: 1 for ICO (2 bytes)
            writeLittleEndianShort(dos, 1); // Number of images (2 bytes)
            
            // 图像目录条目 (16 bytes)
            dos.writeByte(width == 256 ? 0 : width);   // Width (1 byte, 0 means 256)
            dos.writeByte(height == 256 ? 0 : height); // Height (1 byte, 0 means 256)
            dos.writeByte(0);       // Color palette (1 byte, 0 for no palette)
            dos.writeByte(0);       // Reserved (1 byte)
            writeLittleEndianShort(dos, 1); // Color planes (2 bytes)
            writeLittleEndianShort(dos, 32); // Bits per pixel (2 bytes)
            writeLittleEndianInt(dos, pngData.length); // Image data size (4 bytes)
            writeLittleEndianInt(dos, 22); // Image data offset (4 bytes, 6 + 16 = 22)
            
            // 图像数据 (PNG格式)
            dos.write(pngData);
        }
    }

    private static void writeLittleEndianShort(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xFF);
        output.writeByte((value >>> 8) & 0xFF);
    }

    private static void writeLittleEndianInt(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xFF);
        output.writeByte((value >>> 8) & 0xFF);
        output.writeByte((value >>> 16) & 0xFF);
        output.writeByte((value >>> 24) & 0xFF);
    }

    /**
     * 检查文件是否为ICO格式
     *
     * @param file 要检查的文件
     * @return 如果是ICO格式返回true，否则返回false
     */
    public static boolean isIcoFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] header = new byte[6];
            if (input.read(header) != header.length) {
                return false;
            }
            int imageCount = (header[4] & 0xFF) | ((header[5] & 0xFF) << 8);
            return header[0] == 0 && header[1] == 0
                    && header[2] == 1 && header[3] == 0
                    && imageCount > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param filename 完整文件名
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return filename;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    /**
     * 支持的图像格式列表
     */
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

    /**
     * 检查是否为支持的图像格式
     *
     * @param filename 文件名
     * @return 如果是支持的格式返回true，否则返回false
     */
    public static boolean isSupportedImageFormat(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (format.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名（不包含点号）
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
