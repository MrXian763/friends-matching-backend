package com.ziye.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求体
 *
 * @author zicai
 */
@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 8461381816529052507L;

    private long id;
}
