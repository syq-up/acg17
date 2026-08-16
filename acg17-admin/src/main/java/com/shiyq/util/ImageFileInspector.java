package com.shiyq.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Inspects an image from its contents rather than trusting its file name.
 */
public final class ImageFileInspector {

    private static final long MAX_IMAGE_PIXELS = 100_000_000L;

    private ImageFileInspector() {
    }

    /**
     * Reads the image format and dimensions in one ImageIO pass.
     *
     * @param imageFile image file to inspect
     * @return normalized format and image dimensions
     * @throws IOException if the file is missing, invalid, unsupported, or too large
     */
    public static ImageFileInfo inspect(File imageFile) throws IOException {
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

                String format = normalizeFormat(reader.getFormatName());
                return new ImageFileInfo(format, width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    private static String normalizeFormat(String format) throws IOException {
        String normalized = format == null ? "" : format.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(normalized) || "jpg".equals(normalized)) {
            return "jpg";
        }
        if ("png".equals(normalized) || "gif".equals(normalized)
                || "bmp".equals(normalized) || "webp".equals(normalized)) {
            return normalized;
        }
        throw new IOException("不支持的图片格式：" + normalized);
    }

    public record ImageFileInfo(String extension, int width, int height) {
    }
}
