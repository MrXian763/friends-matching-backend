package com.ziye.yupao.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 父标签返回实体类
 */
@Data
public class ParentTagVO implements Serializable {

    /**
     * 标签名
     */
    private String text;

    /**
     * 标签id
     */
    private Long value;

}
