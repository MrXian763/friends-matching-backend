package com.ziye.yupao.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.mapper.UserOnlineStatusMapper;
import com.ziye.yupao.model.domain.UserOnlineStatus;
import com.ziye.yupao.service.UserOnlineStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * @author xianziye
 * @description 针对表【user_online_status(用户在线状态表)】的数据库操作Service实现
 * @createDate 2024-06-18 12:18:22
 */
@Service
public class UserOnlineStatusServiceImpl extends ServiceImpl<UserOnlineStatusMapper, UserOnlineStatus>
        implements UserOnlineStatusService {

    /**
     * 获取用户状态
     *
     * @param id 用户id
     * @return 0-离线 1-在线
     */
    @Override
    public Integer getUserStatus(Long id) {
        if (id == null || id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取用户登录状态
        UserOnlineStatus status = this.getById(id);
        if (status == null) {
            // 新建用户登录状态数据
            status = new UserOnlineStatus();
            status.setUserId(id);
            this.save(status);
        } else {
            return status.getIsOnline();
        }
        return 0;
    }

    /**
     * 更新用户在线状态
     *
     * @param id     用户id
     * @param status 状态 0-离线 1-在线
     * @return 0-设置失败 1-设置成功
     */
    @Override
    public Integer setUserStatus(Long id, Integer status) {
        if (id == null || id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer userStatus = this.getUserStatus(id);
        if (userStatus.equals(status)) {
            return 1;
        }
        UserOnlineStatus userOnlineStatus = new UserOnlineStatus();
        userOnlineStatus.setUserId(id); // 要设置的用户
        userOnlineStatus.setIsOnline(status); // 设置在线状态
        boolean updateResult = this.updateById(userOnlineStatus);
        if (updateResult) {
            return 1;
        }
        return 0;
    }

    /**
     * 更新用户在线状态
     * @param onlineUserIds 在线用户id集合
     */
    @Override
    @Transactional
    public void updateUserOnlineStatus(Set<Long> onlineUserIds) {
        if (onlineUserIds == null || onlineUserIds.isEmpty()) {
            return;
        }
        QueryWrapper<UserOnlineStatus> userOnlineStatusQueryWrapper;
        UserOnlineStatus userOnlineStatus = new UserOnlineStatus();

        // 更新用户状态为在线
        userOnlineStatusQueryWrapper = new QueryWrapper<>();
        userOnlineStatusQueryWrapper.in("user_id", onlineUserIds);
        userOnlineStatusQueryWrapper.eq("is_online", 0);
        userOnlineStatus.setIsOnline(1);
        this.update(userOnlineStatus, userOnlineStatusQueryWrapper);

        // 更新用户状态为离线
        userOnlineStatus.setIsOnline(0);
        userOnlineStatusQueryWrapper = new QueryWrapper<>();
        userOnlineStatusQueryWrapper.notIn("user_id", onlineUserIds);
        userOnlineStatusQueryWrapper.eq("is_online", 1);
        this.update(userOnlineStatus, userOnlineStatusQueryWrapper);
    }

}




