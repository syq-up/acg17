package com.shiyq.entity.VO;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;  // 当前页所有记录
    private long total; // 总记录数
    private long size;  // 页大小
    private long current;   // 当前页数

    public PageVO(long size, long current) {
        this.size = size;
        this.current = current;
    }
}
