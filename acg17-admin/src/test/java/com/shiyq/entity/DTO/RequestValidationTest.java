package com.shiyq.entity.DTO;

import com.shiyq.entity.VO.ReorderRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validatesNovelChapterIdsAndContainerElements() {
        NovelChapterCreateDTO request = new NovelChapterCreateDTO();
        request.setNovelId(0);
        request.setTitle("章节");
        request.setContent(Collections.singletonList(null));

        var messages = validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .toList();

        assertTrue(messages.contains("小说ID必须大于0"));
        assertTrue(messages.contains("章节段落不能为空"));
    }

    @Test
    void validatesRequiredUploadFieldsAndCollectionLimit() {
        GameUploadDTO request = new GameUploadDTO();
        request.setTitle("游戏");
        request.setPreviewImages(new MockMultipartFile[21]);

        var messages = validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .toList();

        assertTrue(messages.contains("游戏封面不能为空"));
        assertTrue(messages.contains("游戏预览图不能超过20张"));
    }

    @Test
    void validatesTagCategoryAndReorderIds() {
        MangaTagCreateDTO tagRequest = new MangaTagCreateDTO();
        tagRequest.setTagName("标签");
        tagRequest.setCategory(7);
        assertTrue(validator.validate(tagRequest).stream()
                .anyMatch(violation -> "标签分类必须在1到6之间".equals(violation.getMessage())));

        ReorderRequest reorderRequest = new ReorderRequest();
        reorderRequest.setId(-1);
        reorderRequest.setTargetId(0);
        assertTrue(validator.validate(reorderRequest).size() == 2);
    }
}
