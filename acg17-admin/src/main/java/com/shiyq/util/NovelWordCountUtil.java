package com.shiyq.util;

import java.util.List;

/**
 * 小说字数统计工具：按 Unicode 字符统计，忽略各类空白，标点计入字数。
 */
public final class NovelWordCountUtil {

    private NovelWordCountUtil() {
    }

    public static int count(List<String> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return 0;
        }

        long total = paragraphs.stream()
                .filter(paragraph -> paragraph != null && !paragraph.isEmpty())
                .flatMapToInt(String::codePoints)
                .filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint))
                .count();
        return Math.toIntExact(total);
    }
}
