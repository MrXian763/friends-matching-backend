package com.ziye.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 *
 * @author zicai
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -3819194788369651084L;

    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当前页码
     */
    protected int pageNum = 1;
}
