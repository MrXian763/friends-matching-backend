package com.ziye.yupao.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserTagChildren implements Serializable {

    /**
     * 子标签名称
     */
    private String text;

    /**
     * 子标签名称
     */
    private String id;

}
