package com.shiyq.entity.VO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ReorderRequest {
    @NotNull(message = "插画ID不能为空")
    @Positive(message = "插画ID必须大于0")
    private Integer id;

    @NotNull(message = "目标插画ID不能为空")
    @Positive(message = "目标插画ID必须大于0")
    private Integer targetId;
}
