package com.shiyq.constant;

public final class MangaConstant {
    // 标签分类
    public static final Integer TAG_CATEGORY_CHARACTER = 1;
    public static final Integer TAG_CATEGORY_MALE = 2;
    public static final Integer TAG_CATEGORY_FEMALE = 3;
    public static final Integer TAG_CATEGORY_MIXED = 4;
    public static final Integer TAG_CATEGORY_OTHER = 5;
    public static final Integer TAG_CATEGORY_ORIGINAL = 6;
    public static final Integer TAG_CATEGORY_ARTIST = 7;
    public static final Integer TAG_CATEGORY_GROUP = 8;

    private MangaConstant() {
    }

    public static boolean isValidCategory(Integer category) {
        return category != null && category >= TAG_CATEGORY_CHARACTER && category <= TAG_CATEGORY_GROUP;
    }

    public static Integer parseCategory(String category) {
        if (category == null) {
            return null;
        }
        switch (category.toLowerCase()) {
            case "1":
            case "character":
                return TAG_CATEGORY_CHARACTER;
            case "2":
            case "male":
                return TAG_CATEGORY_MALE;
            case "3":
            case "female":
                return TAG_CATEGORY_FEMALE;
            case "4":
            case "mixed":
                return TAG_CATEGORY_MIXED;
            case "5":
            case "other":
                return TAG_CATEGORY_OTHER;
            case "6":
            case "original":
                return TAG_CATEGORY_ORIGINAL;
            case "7":
            case "artist":
                return TAG_CATEGORY_ARTIST;
            case "8":
            case "group":
                return TAG_CATEGORY_GROUP;
            default:
                return null;
        }
    }
}
