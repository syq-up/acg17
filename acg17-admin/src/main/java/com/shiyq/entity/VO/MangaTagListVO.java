package com.shiyq.entity.VO;

import lombok.Data;
import java.util.List;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MangaTagListVO {
    private List<MangaTagVO> characterTags;
    private List<MangaTagVO> maleTags;
    private List<MangaTagVO> femaleTags;
    private List<MangaTagVO> mixedTags;
    private List<MangaTagVO> otherTags;
    private List<MangaTagVO> originalTags;
    private List<MangaTagVO> artistTags;
    private List<MangaTagVO> groupTags;
}
