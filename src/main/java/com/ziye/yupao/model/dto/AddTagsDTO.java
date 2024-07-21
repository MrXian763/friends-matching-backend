package com.ziye.yupao.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建标签请求体
 */
@Data
public class AddTagsDTO implements Serializable {

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 父标签名称
     */
    private String parentTagName;

    /**
     * 父标签 id
     */
    private Long parentId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
