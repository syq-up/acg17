package com.shiyq.entity.VO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IllustrationVO {
    private Integer id;
    private String urlTiny; // 缩略图url
    private String urlMiddle;// 全屏展示图url
    private Integer size;
    private Double ratio;
    private Integer sortOrder;
}
