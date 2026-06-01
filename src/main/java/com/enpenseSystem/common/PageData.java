package com.enpenseSystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {

    private Long total; // 总记录数
    private Integer pageNum; // 当前页
    private Integer pageSize; // 每页条数
    private List<T> records; // 当前页数据

    public static <T> PageData<T> empty(Integer pageNum, Integer pageSize) {
        return new PageData<>(0L, pageNum, pageSize, Collections.emptyList());
    }
}
