package com.shiyq.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStyleTest {

    @Test
    void parsesOnlyTheThreeExactStyleNames() {
        assertEquals(MediaStyle.ORIGINAL, MediaStyle.parse("original"));
        assertEquals(MediaStyle.SMALL, MediaStyle.parse("small"));
        assertEquals(MediaStyle.MEDIUM, MediaStyle.parse("medium"));
        assertThrows(IllegalArgumentException.class, () -> MediaStyle.parse("SMALL"));
        assertThrows(IllegalArgumentException.class, () -> MediaStyle.parse(""));
        assertThrows(IllegalArgumentException.class, () -> MediaStyle.parse(null));
    }

    @Test
    void stylesOnlyDownsizeAndNeverUpsize() {
        assertFalse(MediaStyle.ORIGINAL.requiresProcessing(4000, 4000));
        assertFalse(MediaStyle.SMALL.requiresProcessing(450, 450));
        assertTrue(MediaStyle.SMALL.requiresProcessing(451, 200));
        assertFalse(MediaStyle.MEDIUM.requiresProcessing(900, 900));
        assertTrue(MediaStyle.MEDIUM.requiresProcessing(901, 200));
    }
}
