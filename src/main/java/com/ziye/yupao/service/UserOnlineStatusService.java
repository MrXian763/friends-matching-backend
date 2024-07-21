package com.ziye.yupao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ziye.yupao.model.domain.UserOnlineStatus;

import java.util.Set;

/**
* @author xianziye
* @description 针对表【user_online_status(用户在线状态表)】的数据库操作Service
* @createDate 2024-06-18 12:18:22
*/
public interface UserOnlineStatusService extends IService<UserOnlineStatus> {

    /**
     * 获取用户在线状态
     * @param id 用户id
     * @return 0-离线 1-在线
     */
    Integer getUserStatus(Long id);

    /**
     * 更新用户在线状态
     * @param id 用户id
     * @param status 状态 0-离线 1-在线
     * @return 0-设置失败 1-设置成功
     */
    Integer setUserStatus(Long id, Integer status);

    /**
     * 更新用户在线状态
     * @param onlineUserIds 在线用户id集合
     */
    void updateUserOnlineStatus(Set<Long> onlineUserIds);
}
