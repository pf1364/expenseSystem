package com.enpenseSystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象。
 *
 * @param <T> 当前页记录的 VO 类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {

    private Long total; // 总记录数
    private Integer pageNum; // 当前页
    private Integer pageSize; // 每页条数
    private List<T> records; // 当前页数据

    /** 创建指定页码和页大小的空分页结果。 */
    public static <T> PageData<T> empty(Integer pageNum, Integer pageSize) {
        return new PageData<>(0L, pageNum, pageSize, Collections.emptyList());
    }
}
