package com.shiyq.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaImageProcessorTest {

    @TempDir
    Path uploadFolder;

    @Test
    void smallImagesAndOriginalAreReturnedWithoutNativeProcessing() throws Exception {
        Path source = writePng("small.png", 400, 400);
        FakeProcessor processor = new FakeProcessor(uploadFolder);

        MediaImageProcessor.MediaVariant original = processor.process(
                source, "illustrations/1/small.png", MediaStyle.ORIGINAL);
        MediaImageProcessor.MediaVariant small = processor.process(
                source, "illustrations/1/small.png", MediaStyle.SMALL);

        assertSame(source, original.path());
        assertSame(source, small.path());
        assertEquals(0, processor.renderCount.get());
        assertFalse(Files.exists(uploadFolder.resolve(MediaImageProcessor.CACHE_FOLDER)));
    }

    @Test
    void animatedImagesAtTheLimitAreAlsoReturnedByteForByte() throws Exception {
        Path source = writeAnimatedGif("small-animation.gif", 400, 200);
        byte[] original = Files.readAllBytes(source);
        FakeProcessor processor = new FakeProcessor(uploadFolder);

        MediaImageProcessor.MediaVariant variant = processor.process(
                source, "illustrations/1/small-animation.gif", MediaStyle.SMALL);

        assertSame(source, variant.path());
        assertArrayEquals(original, Files.readAllBytes(variant.path()));
        assertEquals(0, processor.renderCount.get());
    }

    @Test
    void cacheIsReusedAndSourceMetadataChangesInvalidateTheKey() throws Exception {
        Path source = writePng("large.png", 801, 600);
        FakeProcessor processor = new FakeProcessor(uploadFolder);

        MediaImageProcessor.MediaVariant first = processor.process(
                source, "illustrations/1/large.png", MediaStyle.SMALL);
        MediaImageProcessor.MediaVariant second = processor.process(
                source, "illustrations/1/large.png", MediaStyle.SMALL);

        assertTrue(first.isDerived());
        assertEquals(first.path(), second.path());
        assertEquals(1, processor.renderCount.get());
        assertTrue(first.path().startsWith(uploadFolder.resolve(MediaImageProcessor.CACHE_FOLDER)));

        Files.setLastModifiedTime(source,
                FileTime.from(Instant.now().plusSeconds(2)));
        MediaImageProcessor.MediaVariant changed = processor.process(
                source, "illustrations/1/large.png", MediaStyle.SMALL);

        assertTrue(changed.isDerived());
        assertNotEquals(first.path(), changed.path());
        assertEquals(2, processor.renderCount.get());
    }

    @Test
    void invalidCachedFileIsReplaced() throws Exception {
        Path source = writePng("replace-cache.png", 801, 600);
        FakeProcessor processor = new FakeProcessor(uploadFolder);
        MediaImageProcessor.MediaVariant first = processor.process(
                source, "illustrations/1/replace-cache.png", MediaStyle.SMALL);
        Files.write(first.path(), new byte[0]);

        MediaImageProcessor.MediaVariant replaced = processor.process(
                source, "illustrations/1/replace-cache.png", MediaStyle.SMALL);

        assertEquals(first.path(), replaced.path());
        assertEquals(4, Files.size(replaced.path()));
        assertEquals(2, processor.renderCount.get());
    }

    @Test
    void nativeLibvipsProducesAStaticBoundedWebpVariantWhenAvailable() throws Exception {
        Assumptions.assumeTrue(vipsCommandAvailable(), "libvips is not installed");
        Path source = writePng("native-large.png", 1200, 600);
        MediaImageProcessor processor = new MediaImageProcessor(uploadFolder);

        MediaImageProcessor.MediaVariant variant = processor.process(
                source, "illustrations/1/native-large.png", MediaStyle.SMALL);

        assertTrue(variant.isDerived());
        assertTrue(variant.path().getFileName().toString().endsWith(".webp"));
        BufferedImage output = ImageIO.read(variant.path().toFile());
        assertEquals(400, output.getWidth());
        assertEquals(200, output.getHeight());
        assertEquals(0, new Color(output.getRGB(200, 100), true).getAlpha());

        Path portrait = writePng("native-portrait.png", 600, 1200);
        MediaImageProcessor.MediaVariant portraitVariant = processor.process(
                portrait, "illustrations/1/native-portrait.png", MediaStyle.SMALL);
        BufferedImage portraitOutput = ImageIO.read(portraitVariant.path().toFile());
        assertEquals(200, portraitOutput.getWidth());
        assertEquals(400, portraitOutput.getHeight());
    }

    @Test
    void nativeLibvipsUsesOnlyTheFirstAnimatedFrameWhenDownsizing() throws Exception {
        Assumptions.assumeTrue(vipsCommandAvailable(), "libvips is not installed");
        Path source = writeAnimatedGif("large-animation.gif", 1200, 600);
        MediaImageProcessor processor = new MediaImageProcessor(uploadFolder);

        MediaImageProcessor.MediaVariant variant = processor.process(
                source, "illustrations/1/large-animation.gif", MediaStyle.SMALL);

        BufferedImage output = ImageIO.read(variant.path().toFile());
        assertEquals(400, output.getWidth());
        assertEquals(200, output.getHeight());
        Color center = new Color(output.getRGB(200, 100), true);
        assertTrue(center.getRed() > center.getBlue());
        try (ImageInputStream input = ImageIO.createImageInputStream(variant.path().toFile())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input);
                assertEquals(1, reader.getNumImages(true));
            } finally {
                reader.dispose();
            }
        }
    }

    private boolean vipsCommandAvailable() throws InterruptedException {
        try {
            Process process = new ProcessBuilder("vips", "--version")
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private Path writePng(String name, int width, int height) throws IOException {
        Path path = uploadFolder.resolve(name);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private Path writeAnimatedGif(String name, int width, int height) throws IOException {
        Path path = uploadFolder.resolve(name);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            writer.writeToSequence(new IIOImage(solidImage(width, height, Color.RED), null, null),
                    writer.getDefaultWriteParam());
            writer.writeToSequence(new IIOImage(solidImage(width, height, Color.BLUE), null, null),
                    writer.getDefaultWriteParam());
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return path;
    }

    private BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static final class FakeProcessor extends MediaImageProcessor {
        private final AtomicInteger renderCount = new AtomicInteger();

        private FakeProcessor(Path uploadFolder) {
            super(uploadFolder);
        }

        @Override
        void renderWebp(Path source, Path temporary, int maxEdge) throws IOException {
            renderCount.incrementAndGet();
            Files.write(temporary, new byte[] { 'W', 'E', 'B', 'P' });
        }
    }
}
