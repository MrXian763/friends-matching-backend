package com.ziye.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求对象
 *
 * @author zicai
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -2618923395748781644L;

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;

}
