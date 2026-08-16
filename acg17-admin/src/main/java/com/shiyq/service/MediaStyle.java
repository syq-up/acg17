package com.shiyq.service;

/**
 * The small, deliberately closed set of image variants exposed by the media
 * endpoint.  The enum is also the single place where variant parsing and
 * sizing rules live, so a request cannot choose arbitrary dimensions or
 * encoder settings.
 */
public enum MediaStyle {
    ORIGINAL(0),
    SMALL(400),
    MEDIUM(800);

    private final int maxEdge;

    MediaStyle(int maxEdge) {
        this.maxEdge = maxEdge;
    }

    public int maxEdge() {
        return maxEdge;
    }

    public boolean requiresProcessing(int width, int height) {
        return this != ORIGINAL && Math.max(width, height) > maxEdge;
    }

    /**
     * Parses the URL value exactly.  In particular, style names are
     * case-sensitive and an unknown value must not silently become original.
     */
    public static MediaStyle parse(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("媒体图片 style 无效");
        }
        return switch (value) {
            case "original" -> ORIGINAL;
            case "small" -> SMALL;
            case "medium" -> MEDIUM;
            default -> throw new IllegalArgumentException("媒体图片 style 无效: " + value);
        };
    }
}
