package com.ziye.yupao.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserTagVO implements Serializable {

    /**
     * 父标签
     */
    private String text;

    /**
     * 子标签
     */
    List<UserTagChildren> children;

}
