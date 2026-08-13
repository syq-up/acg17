package com.shiyq.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovelWordCountUtilTest {

    @Test
    void countsUnicodeCharactersAndIgnoresWhitespace() {
        assertEquals(4, NovelWordCountUtil.count(Collections.singletonList("你 好　😀！")));
    }

    @Test
    void ignoresNullAndAllKindsOfSpaceButKeepsPunctuation() {
        assertEquals(4, NovelWordCountUtil.count(Arrays.asList(null, "\t甲\r\n乙", "\u00A0，。")));
    }

    @Test
    void emptyContentHasNoWords() {
        assertEquals(0, NovelWordCountUtil.count(null));
        assertEquals(0, NovelWordCountUtil.count(Collections.emptyList()));
    }
}
