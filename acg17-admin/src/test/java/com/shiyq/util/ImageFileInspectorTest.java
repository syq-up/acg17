package com.shiyq.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFileInspectorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void detectsPngFromContentsAndReturnsDimensionsDespiteFileExtension() throws Exception {
        Path image = tempDirectory.resolve("image.jpg");
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(source, "png", image.toFile());

        ImageFileInspector.ImageFileInfo info = ImageFileInspector.inspect(image.toFile());

        assertEquals("png", info.extension());
        assertEquals(3, info.width());
        assertEquals(2, info.height());
    }

    @Test
    void normalizesJpegExtension() throws Exception {
        Path image = tempDirectory.resolve("image.any");
        BufferedImage source = new BufferedImage(4, 5, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(source, "jpeg", image.toFile());

        ImageFileInspector.ImageFileInfo info = ImageFileInspector.inspect(image.toFile());

        assertEquals("jpg", info.extension());
        assertEquals(4, info.width());
        assertEquals(5, info.height());
    }

    @Test
    void rejectsInvalidContent() throws Exception {
        Path image = tempDirectory.resolve("not-an-image.bin");
        Files.write(image, "not an image".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> ImageFileInspector.inspect(image.toFile()));
    }

    @Test
    void rejectsImageOverPixelLimitWithoutAllocatingPixelData() throws Exception {
        Path image = tempDirectory.resolve("too-large.png");
        Files.write(image, pngHeader(10_001, 10_000));

        assertThrows(IOException.class, () -> ImageFileInspector.inspect(image.toFile()));
    }

    private static byte[] pngHeader(int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        ByteArrayOutputStream ihdrData = new ByteArrayOutputStream();
        DataOutputStream ihdr = new DataOutputStream(ihdrData);
        ihdr.writeInt(width);
        ihdr.writeInt(height);
        ihdr.writeByte(8);
        ihdr.writeByte(2);
        ihdr.writeByte(0);
        ihdr.writeByte(0);
        ihdr.writeByte(0);
        ihdr.flush();
        writePngChunk(output, "IHDR", ihdrData.toByteArray());
        writePngChunk(output, "IEND", new byte[0]);
        output.flush();
        return bytes.toByteArray();
    }

    private static void writePngChunk(DataOutputStream output, String type, byte[] data)
            throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        output.writeInt(data.length);
        output.write(typeBytes);
        output.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        output.writeInt((int) crc.getValue());
    }
}
