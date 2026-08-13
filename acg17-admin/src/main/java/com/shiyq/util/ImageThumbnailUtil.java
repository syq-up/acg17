package com.shiyq.util;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Rendering;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;

/**
 * 图像缩略图生成工具类
 * 
 * @author shiyongqiang
 * @since 2025-01-16
 *        提供图像等比例缩放生成缩略图的功能
 */
public class ImageThumbnailUtil {

    private static final long MAX_IMAGE_PIXELS = 100_000_000L;

    /**
     * 根据文件内容识别图片格式，不信任文件名或 Content-Type。
     *
     * @param imageFile 图片文件
     * @return 规范化后的扩展名（不含点）
     * @throws IOException 文件不是受支持的有效图片，或图片尺寸异常
     */
    public static String detectImageExtension(File imageFile) throws IOException {
        if (imageFile == null || !imageFile.isFile()) {
            throw new IOException("图片文件不存在");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(imageFile)) {
            if (input == null) {
                throw new IOException("无法读取图片文件：" + imageFile.getName());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("文件内容不是有效图片：" + imageFile.getName());
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IOException("图片尺寸无效或过大：" + imageFile.getName());
                }

                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if ("jpeg".equals(format) || "jpg".equals(format)) {
                    return "jpg";
                }
                if ("png".equals(format) || "gif".equals(format)
                        || "bmp".equals(format) || "webp".equals(format)) {
                    return format;
                }
                throw new IOException("不支持的图片格式：" + format);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * 生成等比例缩略图（文件路径版本）
     * 
     * @param inputPath  输入图片路径
     * @param outputPath 输出缩略图路径
     * @param minSize    最小边尺寸（px）
     * @throws IOException IO异常
     */
    public static void generateThumbnail(String inputPath, String outputPath, int minSize, String formatName) throws IOException {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        generateThumbnail(inputFile, outputFile, minSize, formatName);
    }

    /**
     * 生成等比例缩略图（File对象版本）
     * 
     * @param inputFile  输入图片文件
     * @param outputFile 输出缩略图文件
     * @param minSize    最小边尺寸（px）
     * @throws IOException IO异常
     */
    public static void generateThumbnail(File inputFile, File outputFile, int minSize, String formatName) throws IOException {
        if (!inputFile.exists()) {
            throw new IOException("输入文件不存在：" + inputFile.getAbsolutePath());
        }

        // 确保输出目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        int[] dimensions = getImageDimensions(inputFile);
        int[] targetSize = calculateThumbnailSize(dimensions[0], dimensions[1], minSize);
        String outputFormat = normalizeFormatName(formatName, outputFile);
        Thumbnails.of(inputFile)
                .size(targetSize[0], targetSize[1])
                .outputFormat(outputFormat)
                .outputQuality(getQualityByFormat(outputFormat))
                .rendering(Rendering.QUALITY)
                .toFile(outputFile);
    }

    /**
     * 生成等比例缩略图（流版本）
     * 
     * @param inputStream  输入图片流
     * @param outputStream 输出缩略图流
     * @param minSize      最小边尺寸（px）
     * @param formatName   输出格式（如：JPEG、PNG）
     * @throws IOException IO异常
     */
    public static void generateThumbnail(InputStream inputStream, OutputStream outputStream,
            int minSize, String formatName) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("无法从输入流读取图像");
        }

        int[] targetSize = calculateThumbnailSize(originalImage.getWidth(), originalImage.getHeight(), minSize);
        String outputFormat = normalizeFormatName(formatName, null);
        Thumbnails.of(originalImage)
                .size(targetSize[0], targetSize[1])
                .outputFormat(outputFormat)
                .outputQuality(getQualityByFormat(outputFormat))
                .rendering(Rendering.QUALITY)
                .toOutputStream(outputStream);
    }

    /**
     * 获取图像尺寸信息
     * 
     * @param imagePath 图像文件路径
     * @return 包含宽度和高度的数组 [width, height]
     * @throws IOException IO异常
     */
    public static int[] getImageDimensions(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        return getImageDimensions(imageFile);
    }

    /**
     * 获取图像尺寸信息
     * 
     * @param imageFile 图像文件
     * @return 包含宽度和高度的数组 [width, height]
     * @throws IOException IO异常
     */
    public static int[] getImageDimensions(File imageFile) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(imageFile)) {
            if (in == null) {
                throw new IOException("无法读取图像文件：" + imageFile.getAbsolutePath());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new int[] { reader.getWidth(0), reader.getHeight(0) };
                } finally {
                    reader.dispose();
                }
            }
        }
        throw new IOException("无法解析图像格式：" + imageFile.getAbsolutePath());
    }

    /**
     * 计算缩略图尺寸（不实际生成图像）
     * 
     * @param originalWidth  原始宽度
     * @param originalHeight 原始高度
     * @param minSize        最小边尺寸
     * @return 包含缩略图宽度和高度的数组 [width, height]
     */
    public static int[] calculateThumbnailSize(int originalWidth, int originalHeight, int minSize) {
        if (Math.min(originalWidth, originalHeight) <= minSize) {
            return new int[] { originalWidth, originalHeight };
        }

        int thumbnailWidth, thumbnailHeight;

        if (originalWidth <= originalHeight) {
            thumbnailWidth = minSize;
            thumbnailHeight = (int) Math.round((double) originalHeight * minSize / originalWidth);
        } else {
            thumbnailHeight = minSize;
            thumbnailWidth = (int) Math.round((double) originalWidth * minSize / originalHeight);
        }

        return new int[] { thumbnailWidth, thumbnailHeight };
    }

    /**
     * 根据图片格式适配高质量参数
     * @param format 图片格式
     * @return 输出质量（0.0-1.0）
     */
    private static double getQualityByFormat(String format) {
        String ext = format == null ? "" : format.toUpperCase();
        if (ext.matches("JPG|JPEG|WEBP")) {
            return 0.95;
        } else if (ext.matches("PNG|GIF|BMP")) {
            return 1.0;
        } else {
            return 0.9;
        }
    }

    private static String normalizeFormatName(String formatName, File outputFile) {
        String normalized = formatName;
        if (normalized == null || normalized.trim().isEmpty()) {
            if (outputFile != null) {
                String name = outputFile.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > -1 && dotIndex < name.length() - 1) {
                    normalized = name.substring(dotIndex + 1);
                }
            }
        }
        if (normalized == null || normalized.trim().isEmpty()) {
            normalized = "jpg";
        }
        return normalized.toLowerCase();
    }
}
