package com.cmbchina.orgperformance.vo;

import lombok.Data;

@Data
public class PageVO<T> {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private T data;

    public PageVO(Long total, Integer page, Integer pageSize, T data) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.data = data;
    }
}
