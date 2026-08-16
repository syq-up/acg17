package com.shiyq.entity.VO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IllustrationVO {
    private Integer id;
    private String url;
    private Integer size;
    private Double ratio;
    private Integer sortOrder;
}
